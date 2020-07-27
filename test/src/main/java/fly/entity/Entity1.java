package fly.entity;

import fly.easybeancopier.jar.GenerateFrom;

/**
 * @author FlyInWind
 */
//@GenerateFrom({Entity2.class})
@GenerateFrom({Entity2.class,Entity3.class})
public class Entity1 {
    Integer int1;
    String str1;
    String entity1;

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

    public Entity1 g(Entity2 s) {
        Entity1 t2 = null;
        Entity1 t = new Entity1();
        t.setStr1(s.getStr1());
        return t;
    }
}
