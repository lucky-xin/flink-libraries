package org.apache.flink.streaming.test.examples.entity.enums;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 车型代码
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-05-29
 */
@AllArgsConstructor
@Getter
public enum ModelCode {
    //车型代码MB和M1表示HEV/PHEV/EV车型，车型代码MA和MU表示ICE车型，
    MB("MB", "HEV/PHEV/EV", i -> i.compareTo(BigDecimal.valueOf(11.3)) < 0),
    M1("M1", "HEV/PHEV/EV", i -> i.compareTo(BigDecimal.valueOf(11.3)) < 0),
    MA("MA", "ICE", i -> i.compareTo(BigDecimal.valueOf(11.6)) < 0),
    MU("MU", "ICE", i -> i.compareTo(BigDecimal.valueOf(11.6)) < 0),
    NONE("NONE", "Unknown", i -> false),
    ;

    private final String code;
    private final String name;

    private final Predicate<BigDecimal> alarm;

    public static ModelCode fromModelCode(String code) {
        if (CharSequenceUtil.isEmpty(code)) {
            return NONE;
        }
        String modelCodeInitial = code.substring(0, 2);
        return Stream.of(values())
                .filter(modelCode -> modelCode.getCode().equals(modelCodeInitial))
                .findFirst()
                .orElse(NONE);
    }
}
