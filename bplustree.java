//import all the package which need to use here
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.regex.*;
import java.io.*;

//use generics PECS principle to define the class bplustree
//extend to get, super to insert put
public class bplustree<K extends Comparable<? super K>, V> {

	private static final int Default_MaxNumOfChild = 128; //set a default m-1(m means the order of tree)
	private int MaxNumOfChild; //set a variable m-1, integer type
	private Node root;   //set the root node of the B Plus Tree

	//define the public class bplustree here
	public bplustree() {
		this.MaxNumOfChild = Default_MaxNumOfChild;
	}

	//define a public function initialize function here 
	//to execute initialize function, which is necessary 
	//for constructing a B Plus Tree
	public void initialize(int MaxNumOfChild) {
		if (MaxNumOfChild <= 2)
			throw new IllegalArgumentException("Illegal branching factor: "
					+ MaxNumOfChild);
		this.MaxNumOfChild = MaxNumOfChild;
		root = new LeafNode();
	}
	//define a public function key search here
	public V search(K key) {
		return root.getValue(key);
	}

	//make public the search range function defined later here
	//return the getRange function result
	public List<V> searchRange(K key1, K key2) {
		return root.getRange(key1, key2);
	}


	public void insert(K key, V value) {
		root.insertValue(key, value);
	}


	public void delete(K key) {
		root.deleteValue(key);
	}

	//Make the linked list, as what B+ tree required.
	public String toString() {
		Queue<List<Node>> queue = new LinkedList<List<Node>>();
		queue.add(Arrays.asList(root));
		StringBuilder sb = new StringBuilder();
		while (!queue.isEmpty()) {
			Queue<List<Node>> nextQueue = new LinkedList<List<Node>>();
			while (!queue.isEmpty()) {
				List<Node> nodes = queue.remove();
				sb.append('{');
				Iterator<Node> it = nodes.iterator();
				while (it.hasNext()) {
					Node node = it.next();
					sb.append(node.toString());
					if (it.hasNext())
						sb.append(", ");
					if (node instanceof bplustree.InternalNode)
						nextQueue.add(((InternalNode) node).children);
				}
				sb.append('}');
				if (!queue.isEmpty())
					sb.append(", ");
				else
					sb.append('\n');
			}
			queue = nextQueue;
		}

		return sb.toString();
	}

	//Define an abstract class Node
	private abstract class Node {
		List<K> keys;  //Define keys as List

		int keyNumber() {
			return keys.size();
		}

		abstract V getValue(K key);   

		abstract void deleteValue(K key);

		abstract void insertValue(K key, V value);

		abstract K getFirstLeafKey();

		abstract List<V> getRange(K key1, K key2);

		abstract void merge(Node sibling);

		abstract Node split();

		abstract boolean isOverflow();

		abstract boolean isUnderflow();

		public String toString() {
			return keys.toString();
		}
	}

	//Define a private class Internal Node here
	private class InternalNode extends Node {
		List<Node> children;   //Use a dynamic list children to store internal node

		//Initialze a keys dynamic list and children dynamic list.
		InternalNode() {
			this.keys = new ArrayList<K>();
			this.children = new ArrayList<Node>();
		}
		
		//Return value according to key
		V getValue(K key) {
			return getChild(key).getValue(key);
		}

		//Define deleteValue function here to execute Delete(key) command.
		void deleteValue(K key) {
			Node child = getChild(key);
			child.deleteValue(key);
			if (child.isUnderflow()) {
				Node childLeftSibling = getChildLeftSibling(key);
				Node childRightSibling = getChildRightSibling(key);
				Node left = childLeftSibling != null ? childLeftSibling : child;
				Node right = childLeftSibling != null ? child
						: childRightSibling;
				left.merge(right);
				deleteChild(right.getFirstLeafKey());
				if (left.isOverflow()) {  //check whether the number of left is >m-1
					Node sibling = left.split();  //if it is >m-1, then move the first leaf key to be a child
					insertChild(sibling.getFirstLeafKey(), sibling);
				}
				if (root.keyNumber() == 0)  //check whether the root is deleted
					root = left;    //if it is the case, set the node left to be the root
			}
		}


		//Insert the key into the keys list, and check whether other nodes need to move
		//Since it is the internal node class, as B+ tree required, only store keys, not values.
		void insertValue(K key, V value) {
			Node child = getChild(key);
			child.insertValue(key, value);
			if (child.isOverflow()) {
				Node sibling = child.split();
				insertChild(sibling.getFirstLeafKey(), sibling);
			}
			if (root.isOverflow()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.keys.add(sibling.getFirstLeafKey());
				newRoot.children.add(this);
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		//Return the first leaf key
		K getFirstLeafKey() {
			return children.get(0).getFirstLeafKey();
		}

		List<V> getRange(K key1, K key2) {
			return getChild(key1).getRange(key1, key2);
		}

		//Merge the sibling
		void merge(Node sibling) {
			InternalNode node = (InternalNode) sibling;
			keys.add(node.getFirstLeafKey());
			keys.addAll(node.keys);
			children.addAll(node.children);

		}

		//Define a command split, to separate the left and right sibling
		Node split() {
			int from = keyNumber() / 2 + 1, to = keyNumber();
			InternalNode sibling = new InternalNode();
			sibling.keys.addAll(keys.subList(from, to));
			sibling.children.addAll(children.subList(from, to + 1));
			keys.subList(from - 1, to).clear();
			children.subList(from, to + 1).clear();
			return sibling;  //return the sibling
		}

		//Check whether overflow
		boolean isOverflow() {
			return children.size() > MaxNumOfChild;
		}
		//Check whether underflow
		boolean isUnderflow() {
			return children.size() < (MaxNumOfChild + 1) / 2;
		}

		//Find the index of given key in keys list use binarySearch.
		//childIndex set to loc+1 if result>=0, otherwise set to -loc-1.
		//return the children node in children list use the childIndex.
		Node getChild(K key) {
			int loc = Collections.binarySearch(keys, key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			return children.get(childIndex);
		}

		//Define a function deleteChild here.
		//Use binarySearch to find the location of specific node
		//remove the value according to key
		void deleteChild(K key) {
			int loc = Collections.binarySearch(keys, key);
			if (loc >= 0) {
				keys.remove(loc);
				children.remove(loc + 1);
			}
		}

		//Insert given key and child to specific position.
		void insertChild(K key, Node child) {
			int loc = Collections.binarySearch(keys, key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (loc >= 0) {
				children.set(childIndex, child);
			} else {
				keys.add(childIndex, key);
				children.add(childIndex + 1, child);
			}
		}

		//Use binarySearch to check whether there is already has the given key number
		//If there is already one, return the corresponding element in the list children.
		Node getChildLeftSibling(K key) {
			int loc = Collections.binarySearch(keys, key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (childIndex > 0)
				return children.get(childIndex - 1);

			return null;
		}

		Node getChildRightSibling(K key) {
			int loc = Collections.binarySearch(keys, key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (childIndex < keyNumber())
				return children.get(childIndex + 1);

			return null;
		}
	}

	//Define a private class LeafNode
	private class LeafNode extends Node {
		List<V> values;
		LeafNode next;

		LeafNode() {
			keys = new ArrayList<K>();
			values = new ArrayList<V>();
		}

		//Get the value of the given key
		//Use binarySearch function here to find the index of the specific key
		V getValue(K key) {
			int loc = Collections.binarySearch(keys, key);
			return loc >= 0 ? values.get(loc) : null;
		}


		void deleteValue(K key) {
			int loc = Collections.binarySearch(keys, key);
			if (loc >= 0) {
				keys.remove(loc);
				values.remove(loc);
			}
		}

		//Define insertValue function here
		void insertValue(K key, V value) {
			int loc = Collections.binarySearch(keys, key);
			int valueIndex = loc >= 0 ? loc : -loc - 1;
			if (loc >= 0) {
				values.set(valueIndex, value);
			} else {
				keys.add(valueIndex, key);
				values.add(valueIndex, value);
			}
			if (root.isOverflow()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.keys.add(sibling.getFirstLeafKey());
				newRoot.children.add(this);
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		//Return the first element in keys List.
		K getFirstLeafKey() {
			return keys.get(0);
		}

		//Define a getRange function here use to execute range search.
		//The function finally return every value whose key is key1<=key<=key2 in the List format.
		//import package ahead to use relative function of Iterator
		//use while to check every node
		//use iterator() to ask the container to return Iterator
		//use hasNext() to check whether there is any element in kIt
		//use next() to get the next element in the sequence
		List<V> getRange(K key1, K key2) {
			List<V> result = new LinkedList<V>();
			LeafNode node = this;
			while (node != null) {
				Iterator<K> kIt = node.keys.iterator();
				Iterator<V> vIt = node.values.iterator();
				while (kIt.hasNext()) {
					K key = kIt.next();
					V value = vIt.next();
					int cmp1 = key.compareTo(key1);
					int cmp2 = key.compareTo(key2);
					if ((cmp1 >= 0) && (cmp2 <= 0))
						result.add(value);
					else if (cmp2 > 0)
						return result;
				}
				node = node.next;
			}
			return result;
		}


		void merge(Node sibling) {
			LeafNode node = (LeafNode) sibling;
			keys.addAll(node.keys);   //Add list to list
			values.addAll(node.values);
			next = node.next;
		}

		//Split the sibling
		Node split() {
			LeafNode sibling = new LeafNode();
			int from = (keyNumber() + 1) / 2, to = keyNumber();
			sibling.keys.addAll(keys.subList(from, to));
			sibling.values.addAll(values.subList(from, to));

			keys.subList(from, to).clear();
			values.subList(from, to).clear();

			sibling.next = next;
			next = sibling;
			return sibling;
		}

		//Use boolean store result of whether the number of values exceed m-1
		//m is the tree order, the number of children should not be larger than m-1
		//which is one of the rule of B+ TREE.
		boolean isOverflow() {
			return values.size() > MaxNumOfChild - 1;
		}

		//Same reasons as above.
		boolean isUnderflow() {
			return values.size() < MaxNumOfChild / 2;
		}

	}

  	public static void main(String[] args) 
  	{
		try{
        	String filePath = args[0];
        	FileInputStream fin = new FileInputStream(filePath);
        	InputStreamReader reader = new InputStreamReader(fin);
        	BufferedReader buffReader = new BufferedReader(reader);
        	String strTmp = "";

			BufferedWriter out = new BufferedWriter(new FileWriter("output_file.txt"));
			//to initialize the B Plus Tree
			String str = buffReader.readLine();
			Integer instart = null;
			Integer inend = null;
			for (int i=0; i<str.length(); i++) {
				if (str.substring(i).startsWith("(")) {
					instart = i + 1;
				}
				else if (str.substring(i).startsWith(")")) {
					inend = i;
				}
			}
			String incontent = str.substring(instart, inend);
			Integer treeorder = Integer.parseInt(incontent) + 1;
			bplustree<Integer, Double> bpt = new bplustree<Integer, Double>();
			bpt.initialize(treeorder);
			//System.out.println(treeorder);
        	while((strTmp = buffReader.readLine())!=null){
            	//	System.out.println(strTmp);
				//insert command
				if (strTmp.startsWith("Insert")) {
						int insertlen = strTmp.length();
						Integer istartnum = null;
						Integer isendnum = null;
						for (int i=0; i < insertlen; i++) {
							if (strTmp.substring(i).startsWith("(")) {
								istartnum = i + 1;
							}
							else if (strTmp.substring(i).startsWith(")")) {
								isendnum = i;
							}
						}
						String iscontent = strTmp.substring(istartnum, isendnum);
						String iscSplit[] = iscontent.split(",");
						String istr1 = iscSplit[0].trim();
						String istr2 = iscSplit[1].trim();
						Integer ikey = Integer.parseInt(istr1);
						Double ivalue = Double.parseDouble(istr2);
						bpt.insert(ikey, ivalue); 	//insert to the tree
					}//end of insert command
				//search command
                else if (strTmp.startsWith("Search")) {
					boolean status = strTmp.contains(",");
					if (status) {
						//search range function here
						Integer srstart = null;
						Integer srend = null;
						for (int i=0; i < strTmp.length(); i++) {
							if (strTmp.substring(i).startsWith("(")) {
								srstart = i + 1;
							}
							else if (strTmp.substring(i).startsWith(")")) {
								srend = i;
							}
						}
						String srcontent = strTmp.substring(srstart, srend);
						String srcSplit[] = srcontent.split(",");
						Integer srkey1 = Integer.parseInt(srcSplit[0].trim());
						Integer srkey2 = Integer.parseInt(srcSplit[1].trim());
						List<Double> srresult = bpt.searchRange(srkey1, srkey2);

						//List to string
						StringBuilder sb = new StringBuilder();
						for(int i = 0; i < srresult.size(); i++) {
							sb.append(srresult.get(i)).append(",");
						}
						String realstr = sb.toString().substring(0, sb.toString().length()-1);
						//System.out.println(realstr);
						out.write(realstr);
						out.write("\n");

					} else {		
						Integer sstart = null;
						Integer send = null;
						for (int i=0; i < strTmp.length(); i++) {
							if (strTmp.substring(i).startsWith("(")) {
								sstart = i + 1;
							}
							else if (strTmp.substring(i).startsWith(")")) {
								send = i;
							}
						}
						String scontent = strTmp.substring(sstart, send);
						Integer skey = Integer.parseInt(scontent.trim());
						Double searchresult = bpt.search(skey);

						if (searchresult != null) {
							//System.out.println(searchresult);
							String chresult = Double.toString(searchresult);
							out.write(chresult);
							out.write("\n");
						} else {
							//System.out.println("Null");
							out.write("Null");
							out.write("\n");
						}
					}						 	
				}
				//Delete command
				else if (strTmp.startsWith("Delete")) {
					Integer dstart = null;
					Integer dend = null;
					for (int i=0; i < strTmp.length(); i++) {
						if (strTmp.substring(i).startsWith("(")) {
							dstart = i + 1;
						}
						else if (strTmp.substring(i).startsWith(")")) {
							dend = i;
						}
					}
					String dcontent = strTmp.substring(dstart, dend);
					Integer dkey = Integer.parseInt(dcontent);
					bpt.delete(dkey);
				}
        	}
        	buffReader.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			}
	}
	
}
