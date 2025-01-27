package test;

class TestFaintness {
    /**
     * In this method all variables are faint because the final value is never used.
     * Sample out is at src/test/TestFaintness.Faintness.out
     */
    void test1() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return;
    }

    /**
     * Write your test cases here. Create as many methods as you want.
     * Run the test from root dir using
     * ./run.sh flow.Flow flow.MySolver flow.Faintness test.TestFaintness
     */
    /*
      void test2() {
      }
      ...
    */
}
