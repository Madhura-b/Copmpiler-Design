/**
 * Code developed for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Fall 2020.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Fall 2020 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2020
 *
 */

package cop5556fa20;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import cop5556fa20.CodeGenVisitorComplete;
import cop5556fa20.AST.Program;
import cop5556fa20.CodeGenUtils.DynamicClassLoader;
import cop5556fa20.TypeCheckVisitor.TypeException;
import cop5556fa20.runtime.BufferedImageUtils;
import cop5556fa20.runtime.LoggedIO;
import cop5556fa20.runtime.PLPImage;
import cop5556fa20.runtime.PLPImage.PLPImageException;
import cop5556fa20.runtime.PixelOps;
import cop5556fa20.resources.ImageResources;
import cop5556fa20.resources.ImageResources.*;

class CodeGen6Test {

	static boolean doPrint = true;
	static void show(Object s) {
		if (doPrint) {
			System.out.println(s);
		}
	}


	static boolean doKeepFrames = true;
	static void keepFrames() throws IOException {
		if (doKeepFrames) {
		        System.out.println("enter any char to close frames and exit");
		        int b = System.in.read();
		    }
	}
	
	static boolean writeClassFile = true;
	
	/**
	 * Generates and returns byte[] containing classfile implmenting given input program.
	 * 
	 * Throws exceptions for Lexical, Syntax, and Type checking errors
	 * 
	 * @param input   		String containing source code
	 * @param className		className and fileName of generated code
	 * @return        		Generated bytecode
	 * @throws Exception
	 */
	byte[] genCode(String input, String className, boolean doCreateFile) throws Exception {
		show(input);
		//scan, parse, and type check
		Scanner scanner = new Scanner(input);
		
		scanner.scan();
		Parser parser = new Parser(scanner);
		Program program = parser.parse();
		System.out.println("Ended parsing");
		TypeCheckVisitor v = new TypeCheckVisitor();
		System.out.println("Ended type checking.................................................");
		program.visit(v, className);
		show(program);

		//generate code
		CodeGenVisitorComplete cv = new CodeGenVisitorComplete(className);
		byte[] bytecode = (byte[]) program.visit(cv, null);
		//output the generated bytecode
		show(CodeGenUtils.bytecodeToString(bytecode));
		
		//write byte code to file 
		if (doCreateFile) {
			String classFileName = ImageResources.binDir + File.separator + className + ".class";
			OutputStream output = new FileOutputStream(classFileName);
			output.write(bytecode);
			output.close();
			System.out.println("wrote classfile to " + classFileName);
		}
		
		//return generated classfile as byte array
		return bytecode;
	}
	
	/**
	 * Dynamically loads and executes the main method defined in the provided bytecode.
	 * If there are no command line arguments, commandLineArgs shoudl be an empty string (not null).
	 * 
	 * @param className
	 * @param bytecode
	 * @param commandLineArgs
	 * @throws Exception
	 */
	void runCode(String className, byte[] bytecode, String[] commandLineArgs) throws Exception  {
		LoggedIO.clearGlobalLog(); //initialize log used for testing.
		DynamicClassLoader loader = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> testClass = loader.define(className, bytecode);
		@SuppressWarnings("rawtypes")
		Class[] argTypes = {commandLineArgs.getClass()};
		Method m = testClass.getMethod("main", argTypes );
		show("Command line args: " + Arrays.toString(commandLineArgs));
		show("Output from " + m + ":");  //print name of method to be executed
		Object passedArgs[] = {commandLineArgs};  //create array containing params, in this case a single array.
		try {
		m.invoke(null, passedArgs);	
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				Exception ec = (Exception) e.getCause();
				throw ec;
			}
			throw  e;
		}
	}
	

	String getInputFromFile(String fileName) throws IOException {
		Path path = Path.of(fileName);
		return Files.readString(path);
	}
	
	void genRun(String input, String[] args) throws Exception {
		String classname = name(2);
		byte[] bytecode = genCode(input, classname, writeClassFile);
		runCode(classname, bytecode, args);
	}
	
	void genRun(String input) throws Exception {
		String classname = name(2);
		byte[] bytecode = genCode(input, classname, writeClassFile);
		runCode(classname, bytecode, new String[0]);
	}
	
	
	
	// returns name of method enclosing this one.
	String name() {
		String nameofCurrMethod = new Throwable().getStackTrace()[1].getMethodName();
		return nameofCurrMethod;
	}    
    
    
	// nesting = 0 is method name, 1 is caller, 2 is caller of caller, etc.
	String name(int nesting) {
		String nameofCurrMethod = new Throwable().getStackTrace()[nesting].getMethodName();
		return nameofCurrMethod;
	}
	
	static final int Z=255;
	static final int WHITE = 0xffffffff;
	static final int SILVER = 0xffc0c0c0;
	static final int GRAY=0xff808080;
	static final int BLACK= 0xff000000;
	static final int RED= 0xffff0000;
	static final int MAROON= 0xff800000;
	static final int YELLOW= 0xffffff00;
	static final int OLIVE= 0xff808000;
	static final int LIME= 0xff00ff00;
	static final int GREEN= 0xff008000;
	static final int AQUA= 0xff00ffff;
	static final int TEAL= 0xff008080;
	static final int BLUE= 0xff0000ff;
	static final int NAVY= 0xff000080;
	static final int FUCHSIA= 0xffff00ff;
	static final int PURPLE= 0xff800080;
	
/***********************************************************************/
	
	@Test
	public void loadImage0a() throws Exception {
		String input = """
				image a <- @0;
				a -> screen;
				""";
	    String[] args = {ImageResources.urlTower};
		genRun(input,args);
		ArrayList<Object> expectedLog = new ArrayList<Object>();
		PLPImage a = new PLPImage(BufferedImageUtils.fetchBufferedImage(args[0]),null);
		expectedLog.add(a);
		assertEquals(expectedLog, LoggedIO.globalLog);		
		keepFrames();
	}
	

	
	@Test
	public void loopExampleFromDesc() throws Exception {
		String input = """
				image[400,500] a;
				a = *[X,Y]:X <= Y :red;
				a -> screen;
				""";
		genRun(input);
		PLPImage a = new PLPImage(BufferedImageUtils.createBufferedImage(400, 500), new Dimension(400,500));
		a.ensureImageAllocated(0, 0);
		int w = a.getWidth();
		int h = a.getHeight();
		for (int X = 0; X < w; X++) {
			for (int Y = 0; Y < h; Y++) {
				if (X <= Y) {
					a.updatePixel(X, Y, RED);
				}
			}
		}
		ArrayList<Object> expectedLog = new ArrayList<Object>();
		expectedLog.add(a);
		assertEquals(expectedLog, LoggedIO.globalLog);		
		keepFrames();
	}
	

	@Test void weave() throws Exception {
		String input = """
				image source <- @0;
				int w = source#width;
				int h = source#height;
				int size = w <= h ? w : h;
				image[size,size] overlay <- @1;
				int xoffset = (w-size)/2;
				int yoffset = (h-size)/2;
				image[size,size] checkerboard;
				checkerboard = *[X,Y] :: source[X+xoffset,Y+yoffset];
				int a = 8;
				int b = a/2;
				checkerboard = *[X,Y]: (X%a < b &  Y%a < b) | (b <= X%a  & b <= Y%a)  : overlay[X,Y];
				checkerboard -> screen;
				""";
	    String[] args = {ImageResources.urlKanapaha, ImageResources.urlTower};
		genRun(input, args);
		ArrayList<Object> expectedLog = new ArrayList<Object>();
		PLPImage source = new PLPImage(BufferedImageUtils.fetchBufferedImage(args[0]), null);
		int w = source.getWidth();
		int h = source.getHeight();
		int size = w <=h ? w : h;
		PLPImage overlay = new PLPImage(BufferedImageUtils.resizeBufferedImage(BufferedImageUtils.fetchBufferedImage(args[1]), size, size), new Dimension(size,size));
		PLPImage checkerboard = new PLPImage(BufferedImageUtils.createBufferedImage(size, size), new Dimension(size,size));
		int xoffset = (w-size)/2;
		int yoffset = (h-size)/2;
		for (int X = 0; X < size; X++) {
			for (int Y = 0; Y < size; Y++) {
				checkerboard.updatePixel(X, Y, source.selectPixel(X+xoffset, Y+yoffset));
			}
		}
		int a = 8;
		int b = a/2;
		for (int X = 0; X < size; X++) {
			for (int Y = 0; Y < size; Y++) {
				if ((X%a < b &&  Y%a < b) || (b <= X%a  && b <= Y%a)) {
					checkerboard.updatePixel(X, Y, overlay.selectPixel(X, Y));
				}
			}
		}
		expectedLog.add(checkerboard);
		assertEquals(expectedLog, LoggedIO.globalLog);
		keepFrames();
	}
	


	@Test void hashWidthfail() throws Exception {
		String input = """
				image a;
				int b = a#width;
				""";
		Exception exception = assertThrows(PLPImageException.class, () -> {
			genRun(input);
		});
		show(exception);	
		keepFrames();
	}
	
	
	@Test void hashred() throws Exception {
		
		String className = "HashFunct";
		String input = """
				int b = 100 # red;
				b -> screen;
				""";
		byte[] bytecode = genCode(input, className, false);
		String[] args = {};
		runCode(className, bytecode, args);
		//set up expected log
		
	}
	
@Test void imagetest() throws Exception {
		
		String className = "Imagetest";
		String input = """
				image a;
				""";
		byte[] bytecode = genCode(input, className, false);
		String[] args = {};
		runCode(className, bytecode, args);
		//set up expected log
		
	}


@Test void imagetest3() throws Exception {
	
	String className = "CodeTest";
	String input = """
			string b;
			b = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image[50,50] a <- b;
			a-> screen[70,90];
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}



@Test void imageDec1() throws Exception {
	String className = "CodeTest";
	String input = """
			image a;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec2() throws Exception {
	String className = "CodeTest";
	String input = """
			image[20,30] a;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec3() throws Exception {
	String className = "CodeTest";
	String input = """
			string b;
			b = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image a <- b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec4() throws Exception {
	String className = "CodeTest";
	String input = """
			string b;
			b = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image[30,80] a <- b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec5() throws Exception {
	String className = "CodeTest";
	String input = """
			image b <- "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image a <- b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec6() throws Exception {
	String className = "CodeTest";
	String input = """
			image[70,70] b <- "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image[60,60] a <- b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec7() throws Exception {
	String className = "CodeTest";
	String input = """
			image[70,70] b <- "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image a =b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void imageDec8() throws Exception {
	String className = "CodeTest";
	String input = """
			image[70,70] b <- "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image[70,70] a = b;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void statementOutFileTest() throws Exception {
	String className = "CodeTest";
	String input = """
			string b;
			b = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image a <- b;
			a -> "filename";
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void statementOutScreen() throws Exception {
	String className = "CodeTest";
	String input = """
			string s ="Hi hows it going";
			s -> screen;
			int x = 90;
			x -> screen;
			string c;
			c = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			image[90,90] a <- c;
			a -> screen;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void statementImageInTest() throws Exception {
	String className = "CodeTest";
	String input = """
			image[90,90] a ;
			string x = "http://www.ufl.edu/media/newsufledu/images/generic/azaleas-and-tower.jpg";
			a <- x;
			image b <- a;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {ImageResources.urlTower};
	runCode(className, bytecode, args);
}

@Test void exprCondTest() throws Exception {
	String className = "CodeTest";
	String input = """
			int a = 1!=1 ? 1: 0;
			a -> screen;
			
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {ImageResources.fileImage0, ImageResources.fileImage1};
	runCode(className, bytecode, args);
}

@Test void exprBinaryTest() throws Exception {
	String className = "CodeTest";
	String input = """
			int expo = 9 * 7;
			int exp1 = 2 +6;
			int res = expo / exp1;
			res -> screen;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {ImageResources.fileImage0, ImageResources.fileImage1};
	runCode(className, bytecode, args);
}


@Test void imageConditional1() throws Exception {
	String className = "CodeTest";
	String input = """
			image a <- @0;
			image b <- @1;
			string isEqual = a==b ? "yes" : "no";
			isEqual -> screen;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {ImageResources.urlTower,ImageResources.urlTower};
	runCode(className, bytecode, args);
}

@Test void hashExpr2() throws Exception {
	String className = "CodeTest";
	String input = """
			int r = NAVY # red;
			int g = NAVY # green;
			int b = NAVY # blue;
			r -> screen; //0
			g -> screen; //0
			b -> screen; //128
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}

@Test void statementLoop() throws Exception {
	String className = "CodeTest";
	String input = """
			image[40,50] a;    
			a = *[X,Y]:  X <= Y : RED;
			""";
	byte[] bytecode = genCode(input, className, false);
	String[] args = {};
	runCode(className, bytecode, args);
}
//@Test void Test1() throws Exception {
//	String className = "CodeTest";
//	String input = """
//			imgae a 
//			""";
//	byte[] bytecode = genCode(input, className, false);
//	String[] args = {};
//	runCode(className, bytecode, args);
//}
}
