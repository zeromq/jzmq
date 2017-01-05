package org.zeromq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * ZConfig is a minimal implementation of the <a href="https://rfc.zeromq.org/spec:4/ZPL/">ZeroMQ Property Language</a>
 * 
 * It can put and get values and save and load them to disk:
 * e.g. conf.put("/curve/public-key","abcdef");
 *      String val = conf.get("/curve/public-key","fallback-defaultkey");
 *      conf.save("test.cert");
 *      ZConfig loaded = ZConfig.load("test.cert");
 *      
 *      
 * @author thomas.trocha (at) gmail (dot) com
 *
 */
public class ZConfig {
	
	public static interface IVisitor {
		public void handleNode(ZConfig node,Object ctx,int level);
	}

	private static final Pattern PTRN_CONTAINER = Pattern.compile("^( *)([0-9a-zA-Z\\$\\-_@\\.&\\+\\/]+)( *#.*)?$");
	private static final Pattern PTRN_KEYVALUE = Pattern.compile("^( *)([0-9a-zA-Z\\$\\-_@\\.&\\+\\/]+) = ((\"|')(.*)(\\4)|(.*?))(#.*)?$");
	private static int currentLineNr = 0;	
	
	private String name;
	private String value;
	private ZConfig parent;
//	private List<ZConfig> children = new LinkedList<ZConfig>();
	private HashMap<String,ZConfig> childMap = new HashMap<String, ZConfig>();
	private List<String> comments = new LinkedList<String>();
	
	
	public ZConfig(String name,ZConfig parent) {
		this.parent = parent;
		setName(name);
	}
	
	/**
	 * set name of config item
	 * @param name String
	 */
	public void setName(String name) {
		if (parent!=null){
			if (this.name!=null) {
				parent.childMap.remove(name);
			}
			parent.childMap.put(name, this);
		}
		this.name = name;
	}
	
	/**
	 * 
	 * @return name of config-item
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * 
	 * @param path
	 * @return config-item
	 */
	public String getValue(String path) {
		return getValue(path,null);
	}
	
	/**
	 * 
	 * @param path
	 * @param defaultValue value if no value is set on this path
	 * @return config-item
	 */
	public String getValue(String path,String defaultValue) {
		String[] pathElements = path.split("/");
		ZConfig current = this;
		for (String pathElem : pathElements) {
			if (pathElem.isEmpty()){
				continue;
			}
			current = current.childMap.get(pathElem);
			if (current == null) {
				return defaultValue;
			}
		}
		return current.value;
	}
	
	/**
	 * check if a value-path exists
	 * @param path
	 * @return boolean
	 */
	public boolean pathExists(String path) {
		String[] pathElements = path.split("/");
		ZConfig current = this;
		for (String pathElem : pathElements) {
			if (pathElem.isEmpty()){
				continue;
			}
			current = current.childMap.get(pathElem);
			if (current == null) {
				return false;
			}
		}		
		return true;
	}
	
	/**
	 * add comment
	 * 
	 * @param comment
	 */
	public void addComment(String comment) {
		comments.add(comment);
	}
	
	/**
	 * 
	 * @param value set value of config item
	 */
	public ZConfig putValue(String path,String value ) {
		String[] pathElements = path.split("/");
		ZConfig current = this;
		for(String pathElement : pathElements) {
			if (pathElement.isEmpty()){
				// ignore leading slashes
				continue;
			}
			ZConfig container = current.childMap.get(pathElement);
			if (container == null) {
				container = new ZConfig(pathElement,current);
			}
			current = container;
		}
		current.value = value;
		return current;
	}
	
	private void setValue(String value) {
		this.value = value;
	}
	
	private static void visit(ZConfig startNode,IVisitor handler,Object context,int level){
		handler.handleNode(startNode, context,level);
		for (ZConfig node : startNode.childMap.values()) {
			visit(node,handler,context,level+1);
		}
	}
	
	public void save(String filename) {
		final StringBuffer result = new StringBuffer();
		visit(this,new IVisitor() {
			@Override
			public void handleNode(ZConfig node, Object ctx,int level) {
				// First print comments
				if (node.comments.size()>0) {
					for (String comment : node.comments) {
						result.append("# ").append(comment).append('\n');	
					}
					result.append("\n");
				}
				// now the values
				if (level>0) {
					
					String prefix = level > 1?String.format("%"+((level-1)*4)+"s"," "):"";
					result.append(prefix);
					if (node.value==null){
						result.append(node.name).append("\n");
					} else {
						result.append(String.format("%s = \"%s\"\n", node.name,node.value));
					}
				}
			}
		},null,0);

		if (filename.equals("-")){ // print to stdio
			System.out.println(result.toString());
		} else { // write to file
			try {
				File file = new File(filename);
				if (file.exists()) {
					file.delete();
				} else {
					// create necessary directories;
					try {
						file.getParentFile().mkdirs();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				FileWriter writer = new FileWriter(file);
				writer.write(result.toString());
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static ZConfig load(String filename) {
		
		try {
			currentLineNr = 0;
			ZConfig result = new ZConfig("root",null);
			List<String> content = new ArrayList<String>();
			BufferedReader fr = new BufferedReader(new FileReader(filename));
			String line;
			
			while ( (line=fr.readLine())!=null){
				if ( line.matches("^ *#.*|^ *[0-9]+.*") // ignore comments
					|| line.isEmpty() ) { // ignore empty lines;
						continue;
					}
				
				content.add(line);
			}
			fr.close();
			
			processLoad(result,content,0);
			return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void processLoad(ZConfig node,List<String> content,int currentLevel){
		
		while (currentLineNr < content.size()){
			String currentLine = content.get(currentLineNr);

			Matcher container = PTRN_CONTAINER.matcher(currentLine);
			if (container.find()){
				int containerLevel = container.group(1).length()/4;
				
				if (containerLevel > currentLevel) {
					throw new RuntimeException("Level mismatch in line:"+currentLine);
				}
				else if(containerLevel < currentLevel) {
					break; // jump back;
				}
				currentLineNr++;
				String containerName = container.group(2);
				
				ZConfig zcontainer = new ZConfig(containerName,node);
				processLoad(zcontainer,content,currentLevel+1);
			}
			else {
				Matcher keyvalue = PTRN_KEYVALUE.matcher(currentLine);
				if (keyvalue.find()) {
					int containerLevel = keyvalue.group(1).length()/4;
					
					if (containerLevel != currentLevel) {
						throw new RuntimeException("Level mismatch in line:"+currentLine);
					}
					
					currentLineNr++;
					
					String key = keyvalue.group(2);
					String value = keyvalue.group(5);
					if (value == null) {
						value = keyvalue.group(7);
					}
					
					if (value!=null) {
						value = value.trim();
					}
					
					ZConfig zkeyvalue = new ZConfig(key,node);
					zkeyvalue.setValue(value);
				}
				else {
					throw new RuntimeException("Couldn't process line:"+currentLine);
				}
			}
			
		}
		
		
		
	}
	
}
