import java.util.HashSet;
import java.util.Set;


public class Test {

	public static void main(String[] args) {
		Set<int[]> arr = new HashSet<int[]>();
		int[] x = {1,2,3,4};
		int[] y ={1,3};
		int[] q = {1,2,3,4};
		arr.add(x);
		arr.add(y);
		arr.add(q);
		System.out.println(arr);
	

	}

}
