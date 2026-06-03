-- V5: Add image publishing and tiling related fields to dataset table

ALTER TABLE dataset ADD COLUMN tile_status VARCHAR(20) DEFAULT 'pending';
COMMENT ON COLUMN dataset.tile_status IS '切片状态: pending/processing/completed/failed';

ALTER TABLE dataset ADD COLUMN tile_progress INTEGER DEFAULT 0;
COMMENT ON COLUMN dataset.tile_progress IS '切片进度 0-100';

ALTER TABLE dataset ADD COLUMN tile_job_id VARCHAR(64);
COMMENT ON COLUMN dataset.tile_job_id IS '切片任务ID';

ALTER TABLE dataset ADD COLUMN wms_url VARCHAR(500);
COMMENT ON COLUMN dataset.wms_url IS 'WMS服务地址';

ALTER TABLE dataset ADD COLUMN wmts_url VARCHAR(500);
COMMENT ON COLUMN dataset.wmts_url IS 'WMTS服务地址';

ALTER TABLE dataset ADD COLUMN cache_seed_status VARCHAR(20) DEFAULT 'idle';
COMMENT ON COLUMN dataset.cache_seed_status IS 'GeoWebCache切片状态: idle/seeding/seeded';
