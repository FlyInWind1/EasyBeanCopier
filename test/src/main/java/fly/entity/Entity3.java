package fly.entity;

import fly.processor.beancopier.BeanGenerator;

/**
 * @author FlyInWind
 */
@BeanGenerator({Entity1.class, Entity2.class})
public class Entity3 {
    Integer int1;
    String str1;
    String entity3;

    public Integer getInt1() {
        return int1;
    }

    public void setInt1(Integer int1) {
        this.int1 = int1;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }
}
