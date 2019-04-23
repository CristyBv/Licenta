package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class Table {
    private String keyspace_name;
    private String table_name;
    private Double bloom_filter_fp_chance;
    private Map<String, String> caching;
    private boolean cdc;
    private String comment;
    private Map<String, String> compaction;
    private Map<String, String> compression;
    private Double crc_check_chance;
    private Double dclocal_read_repair_chance;
    private Integer default_time_to_live;
    private Map<String, ByteBuffer> extensions;
    private Set<String> flags;
    private Integer gc_grace_seconds;
    private UUID id;
    private Integer max_index_interval;
    private Integer memtable_flush_period_in_ms;
    private Integer min_index_interval;
    private Double read_repair_chance;
    private String speculative_retry;
}
