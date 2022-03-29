public class Main
{
  private int add(int i) {
    int j = i + 3;
    return j;
  }

  private void run() {
    int x = 2;
    int y = add(x);
    System.out.println(x);
  }

  public Main() {
    run();
  }

  public static void main(String[] args) {
    System.out.println("Welcome to Online IDE!! Happy Coding :)");
    new Main();
  }
}

  //   public static int add(int i) {
  //     int j = i + 3;
  //     return j;
  // }
  