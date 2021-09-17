import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

public class Parser {

	public static String readFileToString(String fpath) {
		Path fileName = Path.of(fpath);
		String fStr;
		try {
			fStr = Files.readString(fileName);
			return fStr;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static void parse(String str) {
		ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
		parser.setSource(str.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		cu.accept(new ASTVisitor() {
					
			private boolean helper(Type nodeType) {
				boolean found = false;
				Code code = null;
				if (nodeType instanceof ParameterizedType) {
					List<Type> argTypes = ((ParameterizedType) nodeType).typeArguments();
					for (Type argType : argTypes) {
						String argStr = argType.toString();
						if (argStr.equals("Double") || argStr.equals("Float")) {
							found = true;
						}
					}
				} else if (nodeType instanceof SimpleType) {
					String argStr = nodeType.toString();
					if (argStr.equals("Double") || argStr.equals("Float")) {
						found = true;
					}
				} else {
					if (nodeType instanceof PrimitiveType) {
						code = ((PrimitiveType) nodeType).getPrimitiveTypeCode();
					} else if (nodeType instanceof ArrayType) {
						nodeType = ((ArrayType) nodeType).getElementType();
						if (nodeType.isPrimitiveType()) {
							code = ((PrimitiveType) nodeType).getPrimitiveTypeCode();
						}
					}
					if (code == PrimitiveType.DOUBLE || code == PrimitiveType.FLOAT) {
						found = true;
					}
				}
				return found;
			}
			
			public boolean visit(MethodDeclaration method) {
				System.out.println(">>> " + method.getName().toString());
				boolean res;
				List<SingleVariableDeclaration> params = method.parameters();
				for (SingleVariableDeclaration param : params) {
					res = helper(param.getType());
					if (res) {
						System.out.println(param.getName() + " at line " + cu.getLineNumber(param.getName().getStartPosition()) + ": Invalid parameter type");
					}
				}
				List<Statement> stmts = method.getBody().statements();
				for (Statement stmt : stmts) {
					if (stmt instanceof VariableDeclarationStatement) {
						res = helper(((VariableDeclarationStatement) stmt).getType());
						if (res) {
							SimpleName name;
							List<VariableDeclarationFragment> frgmts = ((VariableDeclarationStatement) stmt).fragments();
							for (VariableDeclarationFragment f : frgmts) {
								name = f.getName();
								System.out.println(name + " at line " + cu.getLineNumber(name.getStartPosition()) + ": Invalid variable type");
							}
						}
					} else if(stmt instanceof ReturnStatement) {
						Type returnType = method.getReturnType2();
						res = helper(returnType);
						if (res) {
								System.out.println("Invalid return type at line " + cu.getLineNumber(stmt.getStartPosition()));
						}
					}
				}
				return true;
			}
			
			public boolean visit(ImportDeclaration node) {
				System.out.println("Imports: " + node.getName());
				// TODO: Can check against a white/black-list here.
				return true;
			}

			// Check all local variable declarations
//			public boolean visit(VariableDeclarationStatement node) {
//				Code code = null;
//				Type nodeType = node.getType();				
//				if (nodeType instanceof ParameterizedType) {
//					List<Type> argTypes = ((ParameterizedType) nodeType).typeArguments();
//					for (Type argType : argTypes) {
//						String argStr = argType.toString();
//						if (argStr.equals("Double") || argStr.equals("Float")) {
//							System.out.println(argStr + " found");
//						}
//					}
//				} else if (nodeType instanceof SimpleType) {
//					String argStr = nodeType.toString();
//					if (argStr.equals("Double") || argStr.equals("Float")) {
//						System.out.println(argStr + " found");
//					}
//				} else {
//					if (nodeType instanceof PrimitiveType) {
//						code = ((PrimitiveType) nodeType).getPrimitiveTypeCode();
//					} else if (nodeType instanceof ArrayType) {
//						nodeType = ((ArrayType) nodeType).getElementType();
//						if (nodeType.isPrimitiveType()) {
//							code = ((PrimitiveType) nodeType).getPrimitiveTypeCode();
//						}
//					}
//					if (code == PrimitiveType.DOUBLE || code == PrimitiveType.FLOAT) {
//						SimpleName name;
//						List<VariableDeclarationFragment> frgmts = node.fragments();
//						for (VariableDeclarationFragment f : frgmts) {
//							name = f.getName();
//							System.out.println(name + " at line " + cu.getLineNumber(name.getStartPosition()) + " is of type " + code);
//						}								
//					}
//				}
//				return true;
//			}
		});
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fpath = new String(args[0]);
		String fStr = readFileToString(fpath);
		parse(fStr);
	}
}
