package javap_web;

public final class Result {
  private final String result;
  private final String error;

  public String result() {
    return result;
  }

  public String error() {
    return error;
  }

  public Result(String result, String error) {
    this.result = result;
    this.error = error;
  }
}
