"""
Main Agent Service - LangGraph Agent with Memory.

Architecture:
- Short-term Memory: LangGraph Checkpointer (session-scoped)
- Long-term Memory: Supabase (user profile, key facts, summaries)

The agent decides when to use tools vs respond directly.
"""

import logging
import re
from typing import AsyncGenerator, Optional, Annotated, TypedDict
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage, BaseMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import ToolNode, tools_condition

from app.config import settings
from app.tools import ALL_TOOLS
from app.repositories.memory_repository import memory_repository

logger = logging.getLogger(__name__)


# ==================== State Definition ====================

class AgentState(TypedDict):
    """State for the agent graph."""
    messages: Annotated[list[BaseMessage], add_messages]
    user_id: str
    session_id: str


# ==================== Agent Service ====================

class AgentService:
    """Central agent service with memory capabilities."""

    BASE_SYSTEM_PROMPT = """당신은 '누아'라는 이름의 따뜻하고 지혜로운 신앙 도우미입니다.
사용자의 이야기를 경청하고, 공감하며, 적절한 도움을 제공합니다.

## 당신의 역할
- 신앙 상담과 위로
- 성경 구절 검색 및 해석
- 앱 내 화면 네비게이션 안내
- 기도와 묵상 도움

## 도구 사용 가이드
- 사용자가 성경 구절을 찾거나 검색할 때 → bible_search, get_verse 사용
- 사용자가 특정 화면으로 이동하고 싶을 때 → navigate_to_screen 사용
- 사용자에 대해 중요한 정보를 알게 되었을 때 → remember_user_fact 사용
- 단순 대화나 위로가 필요할 때 → 도구 없이 직접 응답

## 응답 스타일
- 따뜻하고 친근한 말투
- 간결하게 (3-4문장)
- 필요시 성경 구절 인용
- 판단하지 않고 경청

## 네비게이션 응답 형식
화면 이동이 필요할 때는 반드시 다음 형식으로 응답:
[NAVIGATE:screen_name] 또는 [NAVIGATE:bible?chapter=3&verse=16]

## 기억하기
사용자에 대해 중요한 정보를 알게 되면 (이름, 관심사, 기도제목, 고민 등)
remember_user_fact 도구를 사용해서 기억해두세요.
"""

    def __init__(self):
        """Initialize the agent with LangGraph."""
        # LLM 설정
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL,
            openai_api_key=settings.OPENAI_API_KEY,
            temperature=0.7,
            max_tokens=1000,
        )

        # Tools (기본 도구 + 메모리 도구)
        self.tools = ALL_TOOLS + [self._create_remember_tool()]

        # LLM with tools
        self.llm_with_tools = self.llm.bind_tools(self.tools)

        # Checkpointer for short-term memory (session-scoped)
        self.checkpointer = MemorySaver()

        # Build the graph
        self.graph = self._build_graph()

    def _create_remember_tool(self):
        """Create tool for remembering user facts."""
        from langchain_core.tools import tool

        @tool
        async def remember_user_fact(
            fact: str,
            category: str = "general"
        ) -> str:
            """
            사용자에 대한 중요한 정보를 기억합니다.

            Args:
                fact: 기억할 내용 (예: "사용자는 요한복음을 좋아한다")
                category: 카테고리 (general, preference, concern, prayer, relationship)

            Returns:
                확인 메시지
            """
            # Note: user_id는 실제 호출 시 context에서 가져옴
            # 여기서는 placeholder로 처리
            return f"기억했습니다: {fact}"

        return remember_user_fact

    def _build_graph(self) -> StateGraph:
        """Build the LangGraph workflow."""

        # Define the agent node
        async def agent_node(state: AgentState) -> dict:
            """Process messages with the LLM."""
            # Build system prompt with memory context
            memory_context = await memory_repository.build_memory_context(
                state.get("user_id", "")
            )

            system_prompt = self.BASE_SYSTEM_PROMPT
            if memory_context:
                system_prompt += f"\n\n## 사용자에 대해 기억하고 있는 정보\n{memory_context}"

            # Prepare messages
            messages = [SystemMessage(content=system_prompt)] + state["messages"]

            # Call LLM
            response = await self.llm_with_tools.ainvoke(messages)

            return {"messages": [response]}

        # Define tool node
        tool_node = ToolNode(tools=self.tools)

        # Build graph
        graph_builder = StateGraph(AgentState)

        # Add nodes
        graph_builder.add_node("agent", agent_node)
        graph_builder.add_node("tools", tool_node)

        # Add edges
        graph_builder.add_edge(START, "agent")
        graph_builder.add_conditional_edges(
            "agent",
            tools_condition,
            {
                "tools": "tools",
                END: END,
            }
        )
        graph_builder.add_edge("tools", "agent")

        # Compile with checkpointer
        return graph_builder.compile(checkpointer=self.checkpointer)

    async def chat(
        self,
        user_message: str,
        session_id: str,
        user_id: str = "",
        conversation_history: list[dict] | None = None
    ) -> dict:
        """
        Process user message with the agent.

        Args:
            user_message: User's message
            session_id: Session ID (used as thread_id for checkpointer)
            user_id: User ID (for long-term memory)
            conversation_history: Previous conversation (optional, for migration)

        Returns:
            dict with 'response', 'navigation' (if any), 'tools_used'
        """
        try:
            # Config for checkpointer
            config = {
                "configurable": {
                    "thread_id": session_id,
                }
            }

            # Initial state
            initial_state = {
                "messages": [HumanMessage(content=user_message)],
                "user_id": user_id,
                "session_id": session_id,
            }

            # If we have conversation history and this is a new thread, populate it
            # This handles migration from old sessions
            if conversation_history:
                existing_state = await self.graph.aget_state(config)
                if not existing_state.values.get("messages"):
                    # Populate with history
                    history_messages = []
                    for msg in conversation_history[-10:]:
                        if msg.get("role") == "user":
                            history_messages.append(HumanMessage(content=msg["content"]))
                        elif msg.get("role") == "assistant":
                            history_messages.append(AIMessage(content=msg["content"]))
                    initial_state["messages"] = history_messages + initial_state["messages"]

            # Run the graph
            result = await self.graph.ainvoke(initial_state, config)

            # Get the last AI message
            ai_messages = [
                m for m in result["messages"]
                if isinstance(m, AIMessage)
            ]
            if not ai_messages:
                return {
                    "response": "죄송합니다. 응답을 생성하지 못했어요.",
                    "navigation": None,
                    "tools_used": [],
                }

            last_message = ai_messages[-1]
            output = last_message.content

            # Parse navigation
            navigation = self._parse_navigation(output)

            # Clean response
            clean_response = self._clean_response(output)

            # Extract tools used
            tools_used = self._get_tools_used(result["messages"])

            # Save key facts if remember_user_fact was called
            await self._process_tool_calls(result["messages"], user_id)

            return {
                "response": clean_response,
                "navigation": navigation,
                "tools_used": tools_used,
            }

        except Exception as e:
            logger.error(f"Agent error: {e}", exc_info=True)
            return {
                "response": "죄송합니다. 잠시 문제가 발생했어요. 다시 말씀해 주시겠어요?",
                "navigation": None,
                "tools_used": [],
            }

    async def chat_stream(
        self,
        user_message: str,
        session_id: str,
        user_id: str = "",
        conversation_history: list[dict] | None = None
    ) -> AsyncGenerator[str, None]:
        """
        Stream chat response.

        Args:
            user_message: User's message
            session_id: Session ID
            user_id: User ID
            conversation_history: Previous conversation

        Yields:
            Response chunks
        """
        try:
            # Use non-streaming for now, chunk the response
            result = await self.chat(
                user_message, session_id, user_id, conversation_history
            )

            response = result["response"]
            words = response.split(" ")

            for i, word in enumerate(words):
                if i > 0:
                    yield " "
                yield word

        except Exception as e:
            logger.error(f"Streaming error: {e}", exc_info=True)
            yield "죄송합니다. 오류가 발생했어요."

    async def summarize_and_close_session(
        self,
        session_id: str,
        user_id: str
    ) -> bool:
        """
        Summarize a session and store in long-term memory.

        Call this when a session is ending or after significant conversations.

        Args:
            session_id: Session ID
            user_id: User ID

        Returns:
            True if successful
        """
        try:
            # Get session state
            config = {"configurable": {"thread_id": session_id}}
            state = await self.graph.aget_state(config)

            if not state.values.get("messages"):
                return False

            messages = state.values["messages"]

            # Build conversation text
            conversation_text = ""
            for msg in messages:
                if isinstance(msg, HumanMessage):
                    conversation_text += f"사용자: {msg.content}\n"
                elif isinstance(msg, AIMessage):
                    conversation_text += f"누아: {msg.content}\n"

            if not conversation_text:
                return False

            # Use LLM to summarize
            summary_prompt = f"""다음 대화를 2-3문장으로 요약해주세요.
주요 주제와 사용자의 감정/상태를 포함해주세요.

대화:
{conversation_text}

요약:"""

            response = await self.llm.ainvoke([HumanMessage(content=summary_prompt)])
            summary = response.content

            # Extract topics (simple keyword extraction)
            topics = self._extract_topics(conversation_text)

            # Save summary
            await memory_repository.save_conversation_summary(
                user_id=user_id,
                session_id=session_id,
                summary=summary,
                topics=topics,
            )

            return True

        except Exception as e:
            logger.error(f"Failed to summarize session: {e}", exc_info=True)
            return False

    def _extract_topics(self, text: str) -> list[str]:
        """Extract main topics from conversation text."""
        topic_keywords = {
            "성경": "성경공부",
            "기도": "기도",
            "감사": "감사",
            "걱정": "고민상담",
            "힘들": "위로",
            "스트레스": "스트레스",
            "직장": "직장",
            "가족": "가족",
            "건강": "건강",
            "신앙": "신앙",
        }

        topics = []
        for keyword, topic in topic_keywords.items():
            if keyword in text and topic not in topics:
                topics.append(topic)

        return topics[:5]  # Max 5 topics

    async def _process_tool_calls(
        self,
        messages: list[BaseMessage],
        user_id: str
    ) -> None:
        """Process tool calls and save facts to long-term memory."""
        for msg in messages:
            if isinstance(msg, AIMessage) and msg.tool_calls:
                for tool_call in msg.tool_calls:
                    if tool_call["name"] == "remember_user_fact":
                        args = tool_call["args"]
                        await memory_repository.add_key_fact(
                            user_id=user_id,
                            fact=args.get("fact", ""),
                            category=args.get("category", "general"),
                        )

    def _parse_navigation(self, output: str) -> Optional[dict]:
        """Parse navigation command from agent output."""
        pattern = r'\[NAVIGATE:([^\]]+)\]'
        match = re.search(pattern, output)

        if match:
            nav_string = match.group(1)

            if '?' in nav_string:
                screen, params_str = nav_string.split('?', 1)
                params = dict(
                    p.split('=') for p in params_str.split('&') if '=' in p
                )
            else:
                screen = nav_string
                params = {}

            return {
                "screen": screen,
                "params": params,
            }

        return None

    def _clean_response(self, output: str) -> str:
        """Remove navigation tags from response."""
        cleaned = re.sub(r'\[NAVIGATE:[^\]]+\]\s*', '', output)
        return cleaned.strip()

    def _get_tools_used(self, messages: list[BaseMessage]) -> list[str]:
        """Extract list of tools used from messages."""
        tools_used = []
        for msg in messages:
            if isinstance(msg, AIMessage) and msg.tool_calls:
                for tool_call in msg.tool_calls:
                    if tool_call["name"] not in tools_used:
                        tools_used.append(tool_call["name"])
        return tools_used


# Global service instance
agent_service = AgentService()
