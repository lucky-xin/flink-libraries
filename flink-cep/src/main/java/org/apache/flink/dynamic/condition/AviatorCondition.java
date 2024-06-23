package org.apache.flink.dynamic.condition;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.Getter;
import org.apache.flink.annotation.Internal;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.util.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Condition that accepts aviator expression.
 * @author luchaoxin
 */
@Internal
public class AviatorCondition<T> extends SimpleCondition<T> {

    private static final long serialVersionUID = 1L;

    /**
     * The filter expression of the condition.
     */
    @Getter
    private final String expression;

    private transient Expression compiledExpression;

    public AviatorCondition(String expression) {
        this(expression, null);
    }

    public AviatorCondition(String expression, @Nullable String filterField) {
        this.expression =
                StringUtils.isNullOrWhitespaceOnly(filterField)
                        ? requireNonNull(expression)
                        : filterField + requireNonNull(expression);
        checkExpression(this.expression);
    }

    @Override
    public boolean filter(T eventBean) throws Exception {
        if (compiledExpression == null) {
            // Compile the expression when it is null to allow static CEP to use AviatorCondition.
            compiledExpression = AviatorEvaluator.compile(expression, false);
        }
        try {
            List<String> variableNames = compiledExpression.getVariableNames();
            if (variableNames.isEmpty()) {
                return true;
            }
            Map<String, Object> variables = new HashMap<>(variableNames.size());
            for (String variableName : variableNames) {
                Object variableValue = getVariableValue(eventBean, variableName);
                if (!Objects.isNull(variableValue)) {
                    variables.put(variableName, variableValue);
                }
            }
            return (Boolean) compiledExpression.execute(variables);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            // If we find that some fields reside in the expression but does not appear in the
            // eventBean, we directly return false. Because we would consider the existence of the
            // field is an implicit condition (i.e. AviatorCondition("a > 10") is equivalent to
            // AviatorCondition("a exists && a > 10").
            return false;
        }
    }

    private void checkExpression(String expression) {
        try {
            AviatorEvaluator.validate(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The expression of AviatorCondition is invalid: " + e.getMessage());
        }
    }

    private Object getVariableValue(T propertyBean, String variableName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = propertyBean.getClass().getDeclaredField(variableName);
        field.setAccessible(true);
        return field.get(propertyBean);
    }
}
