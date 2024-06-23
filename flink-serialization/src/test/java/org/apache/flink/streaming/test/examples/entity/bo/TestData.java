package org.apache.flink.streaming.test.examples.entity.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.flink.model.IKafkaRecord;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * TestData
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-05-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestData implements IKafkaRecord {
    @JsonProperty(value = "vin", required = true)
    @NotBlank
    private String vin;

    @JsonProperty(value = "sampleTime", required = true)
    @NotNull
    private Long sampleTime;

    @JsonProperty(value = "series", required = true)
    @NotBlank
    private String series;

    @JsonProperty(value = "modelCode", required = true)
    @Pattern(regexp = "^(MA|MB|M1|MU)[\\s\\S]+$")
    @NotBlank
    private String modelCode;

    @JsonProperty(value = "type", required = true)
    @NotBlank
    private String msgType;

    @JsonProperty(value = "content", required = true)
    private DataContent content;

    /**
     * Kafka 分区
     */
    private int partition;

    /**
     * Kafka 偏移量
     */
    private long offset;

    @NoArgsConstructor
    @Data
    public static class DataContent implements Serializable {

        @JsonProperty(value = "KeySt")
        private Byte keyst;


        @JsonProperty(value = "LatestLockUnlockCmd")
        private Byte latestLockUnlockCmd;

        @JsonProperty(value = "Reissue")
        private Byte reissue;

        @JsonProperty(value = "BattCurrent")
        private BigDecimal battCurrent;


        @JsonProperty(value = "HazardLampSt")
        private Byte hazardLampSt;

        @JsonProperty(value = "HighBeamCmd")
        private Byte highBeamCmd;

        @JsonProperty(value = "LowBeamCmd")
        private Byte lowBeamCmd;

        @JsonProperty(value = "FrontFogLampSt")
        private Byte frontFogLampSt;

        @JsonProperty(value = "RearFogLampSt")
        private Byte rearFogLampSt;

        @JsonProperty(value = "DriverDoorAjarSt")
        private Byte driverDoorAjarSt;

        @JsonProperty(value = "PsngrDoorAjarSt")
        private Byte psngrDoorAjarSt;

        @JsonProperty(value = "RLDoorAjarSt")
        private Byte rlDoorAjarSt;

        @JsonProperty(value = "RRDoorAjarSt")
        private Byte rrdooraJarSt;

        @JsonProperty(value = "TrunkAjarSt")
        private Byte trunkAjarSt;

        @JsonProperty(value = "BonnetAjarSt")
        private Byte bonnetAjarsSt;

        @JsonProperty(value = "WINDEXITSPD")
        private Byte windexitspd;

        @JsonProperty(value = "SOC")
        private Byte soc;

        @JsonProperty(value = "SOC_STATE")
        private Byte socState;

        @JsonProperty(value = "BattVolt")
        private BigDecimal battVolt;

        @JsonProperty(value = "CurrentRange")
        private Byte currentRange;

        @JsonProperty(value = "FLHVSMAutoModeSt")
        private Byte flHvsmAutoModeSt;

        @JsonProperty(value = "FRHVSMAutoModeSt")
        private Byte frHvsmAutoModeSt;

        @JsonProperty(value = "MCMRL_MasSt")
        private Byte rlMasSt;

        @JsonProperty(value = "MCMRR_MasSt")
        private Byte rrMasSt;

        @JsonProperty(value = "MCMFL_MasSt")
        private Byte flMasSt;

        @JsonProperty(value = "MCMFR_MasSt")
        private Byte frMasSt;

        @JsonProperty(value = "FLHeatingActLevel")
        private Byte flHeatingActLevel;

        @JsonProperty(value = "FLVentilatingActLevel")
        private Byte flVentilatingActLevel;

        @JsonProperty(value = "FRVentilatingActLevel")
        private Byte frVentilatingActLevel;

        @JsonProperty(value = "FRHeatingActLevel")
        private Byte frHeatingActLevel;

        @JsonProperty(value = "DriverDoorLockSt")
        private Byte driverDoorLockSt;

        @JsonProperty(value = "PsngrDoorLockSt")
        private Byte psngrDoorLockSt;
    }
}
