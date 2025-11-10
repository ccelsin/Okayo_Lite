
package backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import backend.utilities.BeanCopyUtils;

class BeanCopyUtilsTest {

    static class A {
        String a; String b; String c;
        A(String a, String b, String c){ this.a=a; this.b=b; this.c=c; }
        public String getA(){return a;} public void setA(String v){a=v;}
        public String getB(){return b;} public void setB(String v){b=v;}
        public String getC(){return c;} public void setC(String v){c=v;}
    }

    @Test
    void copyNonNullProperties_ignoreNullsAndExtra() {
        // source : a=NEW, b=null (ne doit pas écraser), c=NEW
        A source = new A("NEW", null, "NEWC");
        // target initial
        A target = new A("OLD", "KEEP", "OLDC");

        BeanCopyUtils.copyNonNullProperties(source, target, "c"); // c doit être ignoré

        assertThat(target.getA()).isEqualTo("NEW");   // copié
        assertThat(target.getB()).isEqualTo("KEEP");  // non écrasé par null
        assertThat(target.getC()).isEqualTo("OLDC");  // ignoré explicitement
    }
}
