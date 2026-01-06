-- Supabase Dashboard > SQL Editor에서 실행하세요

-- 1. sermons 테이블 생성
CREATE TABLE IF NOT EXISTS sermons (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    youtube_video_id TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    preacher TEXT,
    description TEXT,
    scripture TEXT,
    thumbnail_url TEXT,
    published_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_sermons_published_at ON sermons(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_sermons_is_active ON sermons(is_active);

-- 3. RLS (Row Level Security) 활성화
ALTER TABLE sermons ENABLE ROW LEVEL SECURITY;

-- 4. 읽기 정책: 모든 사용자가 활성 설교 조회 가능
CREATE POLICY "Anyone can view active sermons"
ON sermons FOR SELECT
USING (is_active = TRUE);

-- 5. youtube_channel_config 테이블 (채널 설정 저장)
CREATE TABLE IF NOT EXISTS youtube_channel_config (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    channel_id TEXT NOT NULL UNIQUE,
    channel_name TEXT,
    default_preacher TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. 채널 설정 RLS
ALTER TABLE youtube_channel_config ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view channel config"
ON youtube_channel_config FOR SELECT
USING (is_active = TRUE);

-- 7. updated_at 자동 업데이트 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 8. 트리거 적용
CREATE TRIGGER update_sermons_updated_at
    BEFORE UPDATE ON sermons
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 9. 채널 설정
INSERT INTO youtube_channel_config (channel_id, channel_name, default_preacher)
VALUES ('UCJ2eYxs8KiyXMCUDAO10nUQ', '테스트교회', '홍길동 목사')
ON CONFLICT (channel_id) DO NOTHING;
