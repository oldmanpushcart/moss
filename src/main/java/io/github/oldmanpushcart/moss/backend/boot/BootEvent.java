package io.github.oldmanpushcart.moss.backend.boot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@Value
public class BootEvent extends ApplicationEvent {

    int total;
    int loaded;
    String tips;

    public BootEvent(Object source, int total, int loaded, String tips) {
        super(source);
        this.total = total;
        this.loaded = loaded;
        this.tips = tips;
    }

    public double progress() {
        return loaded * 1.0 / total;
    }

}
