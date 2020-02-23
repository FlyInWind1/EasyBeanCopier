package fly.processor.beancopier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author FlyInWind
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface BeanGenerator {
    /**
     * Types to copy from
     */
    Class<?>[] value();
}