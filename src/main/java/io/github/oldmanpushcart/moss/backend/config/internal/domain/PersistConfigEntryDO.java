package io.github.oldmanpushcart.moss.backend.config.internal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@ToString
@EqualsAndHashCode
@Accessors(chain = true)
public class PersistConfigEntryDO {
    private String key;
    private String value;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
