package org.apache.flink.dynamic.condition;

import lombok.Getter;
import org.apache.flink.annotation.Internal;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.util.StringUtils;
import org.mvel2.MVEL;

import javax.annotation.Nullable;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Condition that accepts aviator expression.
 *
 * @author luchaoxin
 */
@Internal
public class MVELCondition<T> extends SimpleCondition<T> {

    private static final long serialVersionUID = 1L;

    /**
     * The filter expression of the condition.
     */
    @Getter
    private final String expression;

    private transient Serializable compiledExpression;

    public MVELCondition(String expression) {
        this(expression, null);
    }

    public MVELCondition(String expression, @Nullable String filterField) {
        this.expression =
                StringUtils.isNullOrWhitespaceOnly(filterField)
                        ? requireNonNull(expression)
                        : filterField + requireNonNull(expression);
        checkExpression(this.expression);
    }

    @Override
    public boolean filter(T eventBean) throws Exception {
        if (compiledExpression == null) {
            // Compile the expression when it is null to allow static CEP to use MVELCondition.
            this.compiledExpression = MVEL.compileExpression(expression);
        }
        return MVEL.executeExpression(compiledExpression, eventBean, Boolean.class);
    }

    private void checkExpression(String expression) {
        try {
            MVEL.compileExpression(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The expression of MVELCondition is invalid: " + e.getMessage());
        }
    }
}
