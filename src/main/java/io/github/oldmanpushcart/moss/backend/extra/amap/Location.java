package io.github.oldmanpushcart.moss.backend.extra.amap;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("经纬度坐标")
public record Location(

        @JsonPropertyDescription("经度")
        @JsonProperty(required = true)
        Double longitude,

        @JsonPropertyDescription("纬度")
        @JsonProperty(required = true)
        Double latitude

) {

    @Override
    public String toString() {
        return "%.6f,%.6f".formatted(longitude, latitude);
    }

}
