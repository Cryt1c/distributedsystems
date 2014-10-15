package example;

public class Main {

	public static void main(String[] args) throws Exception {
		// Create a new factory
		ComponentFactory factory = new ComponentFactory();
		// Retrieve a new instance of shell-example
		IShellExampleCli example = factory.createShellExample("shell-example",
				System.in, System.out);
		// Start the instance in a new thread
		new Thread((Runnable) example).start();
	}

}
