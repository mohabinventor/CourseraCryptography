package week5;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Your goal this week is to write a program to compute discrete log modulo a
 * prime <b><i>p</i></b>. Let <b><i>g</i></b> be some element in
 * Z<sup>*</sup><sub>p</sub> and suppose you are given h in
 * Z<sup>*</sup><sub>p</sub> such that <code>h=g<sup>x</sup></code> where
 * <code>1 ≤ x ≤ 240</code>. Your goal is to find <b><i>x</i></b>. More
 * precisely, the input to your program is <b><i>p, g, h</i></b> and the output
 * is <b><i>x</i></b>.
 * <p>
 * The trivial algorithm for this problem is to try all 2<sup>40</sup> possible
 * values of <b><i>x</i></b> until the correct one is found, that is until we
 * find an x satisfying <code>h=g<sup>x</sup></code> in Z<sub>p</sub>. This
 * requires 2<sup>40</sup> multiplications. In this project you will implement
 * an algorithm that runs in time roughly √2<sup>40</sup>=2<sup>20</sup> using a
 * meet in the middle attack.
 * <p>
 * Let B=2<sup>20</sup>. Since <b><i>x</i></b> is less than B<sup>2</sup> we can
 * write the unknown <b><i>x</i></b> base B as
 * <b><i>x</i></b>=x<sub>0</sub>B+x<sub>1</sub> where x<sub>0</sub>,
 * x<sub>1</sub> are in the range [0,B−1]. Then
 * <p>
 * <code>h=g<sup>x</sup>=g<sup>x<sub>0</sub>B+x<sub>1</sub></sup>=(g<sup>B</sup>)<sup>x<sub>0</sub></sup>⋅g<sup>x<sub>1</sub></sup> in Z<sub>p</sub>.</code>
 * <p>
 * By moving the term g<sup>x<sub>1</sub></sup> to the other side we obtain
 * <p>
 * <code>h/g<sup>x<sub>1</sub></sup>=(g<sup>B</sup>)<sup>x<sub>0</sub></sup> in Z<sub>p</sub>.</code>
 * <p>
 * The variables in this equation are x<sub>0</sub>, x<sub>1</sub> and
 * everything else is known: you are given g,h and B=2<sup>20</sup>. Since the
 * variables x<sup>0</sup> and x<sup>1</sup> are now on different sides of the
 * equation we can find a solution using meet in the middle (<a
 * href="https://class.coursera.org/crypto-005/lecture/view?lecture_id=14"
 * >Lecture 3.3</a>):
 * <ol>
 * <li>First build a hash table of all possible values of the left hand side
 * <code>h/g<sup>x<sub>1</sub></sup></code> for
 * x<sub>1</sub>=0,1,…,2<sup>20</sup>.</li>
 * <li>Then for each value x<sub>0</sub>=0,1,2,…,2<sup>20</sup> check if the
 * right hand side (g<sup>B</sup>)<sup>x<sub>0</sub></sup> is in this hash
 * table. If so, then you have found a solution (x<sub>0</sub>,x<sub>1</sub>)
 * from which you can compute the required x as x<sub>0</sub>B+x<sub>1</sub>.</li>
 * </ol>
 * The overall work is about 2<sup>20</sup> multiplications to build the table
 * and another 2<sup>20</sup> lookups in this table.
 * 
 * Now that we have an algorithm, here is the problem to solve:
 * <p>
 * <code>
 * p = 134078079299425970995740249982058461274793658205923933 \ <br>
 * 77723561443721764030073546976801874298166903427690031 \ <br>
 * 858186486050853753882811946569946433649006084171
 * <br>
 * g = 11717829880366207009516117596335367088558084999998952205 \ <br>
 * 59997945906392949973658374667057217647146031292859482967 \ <br>
 * 5428279466566527115212748467589894601965568
 * <br>
 * h = 323947510405045044356526437872806578864909752095244 \ <br>
 * 952783479245297198197614329255807385693795855318053 \ <br>
 * 2878928001494706097394108577585732452307673444020333
 * </code>
 * <p>
 * Each of these three numbers is about 153 digits. Find <b><i>x</i></b> such
 * that <code>h=g<sup>x</sup></code> in Z<sub>p</sub>.
 * 
 * To solve this assignment it is best to use an environment that supports
 * multi-precision and modular arithmetic. In Python you could use the gmpy2 or
 * numbthy modules. Both can be used for modular inversion and exponentiation.
 * In C you can use GMP. In Java use a {@link BigInteger} class which can
 * perform {@link BigInteger#mod}, {@link BigInteger#modPow} and
 * {@link BigInteger#modInverse} operations.
 * <p>
 * <b>Solution</b>
 * <p>
 * Implementation of meet in the middle attack for DLog. Tasks for building hash
 * table and searching in this table are splitted to execute in separate
 * threads.
 * <p>
 * <b>375374217830</b>
 * 
 * @author rustam
 * 
 */
public class Prog5 {
	private static final BigInteger p = new BigInteger(
			"13407807929942597099574024998205846127479365820592393377723561443721764030073546976801874298166903427690031858186486050853753882811946569946433649006084171");
	private static final BigInteger g = new BigInteger(
			"11717829880366207009516117596335367088558084999998952205599979459063929499736583746670572176471460312928594829675428279466566527115212748467589894601965568");
	private static final BigInteger h = new BigInteger(
			"3239475104050450443565264378728065788649097520952449527834792452971981976143292558073856937958553180532878928001494706097394108577585732452307673444020333");
	private static final int B = (int) Math.pow(2, 20);
	private static ExecutorService executor;

	/**
	 * Class represents the thread task for building hash table
	 * 
	 * @author rustam
	 * 
	 */
	public static class BuildHashTable implements
			Callable<Map<BigInteger, BigInteger>> {
		private final int from;
		private final int to;
		private final Map<BigInteger, BigInteger> left = new HashMap<BigInteger, BigInteger>();

		public BuildHashTable(int from, int to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public Map<BigInteger, BigInteger> call() throws Exception {
			for (int i = from; !Thread.interrupted() && i < to; i++) {
				BigInteger exp = new BigInteger(Integer.toString(i));
				BigInteger gx1 = g.modInverse(p).modPow(exp, p);
				left.put(h.multiply(gx1).mod(p), exp);
			}
			return left;
		}
	};

	public static class SearchInHashTable implements
			Callable<Map.Entry<BigInteger, BigInteger>> {
		private final int from;
		private final int to;
		private final Map<BigInteger, BigInteger>[] xx;

		public SearchInHashTable(int from, int to,
				Map<BigInteger, BigInteger>[] xx) {
			this.from = from;
			this.to = to;
			this.xx = xx;
		}

		@Override
		public Map.Entry<BigInteger, BigInteger> call() throws Exception {
			for (int i = from; !Thread.interrupted() && i < to; i++) {
				BigInteger exp = new BigInteger(Long.toString(i))
						.multiply(new BigInteger(Long.toString(B)));
				BigInteger gbx1 = g.modPow(exp, p);
				for (int k = 0; k < xx.length; k++)
					if (xx[k].containsKey(gbx1))
						return new AbstractMap.SimpleEntry<BigInteger, BigInteger>(
								exp, xx[k].get(gbx1));
			}
			return null;
		}
	}

	/**
	 * @param args
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException,
			ExecutionException {
		// cpu count
		int threadCnt = Runtime.getRuntime().availableProcessors();
		threadCnt = threadCnt - threadCnt % 2;
		int tmp = B / threadCnt;
		executor = Executors.newFixedThreadPool(threadCnt);
		Queue<Future<Map<BigInteger, BigInteger>>> futures = new LinkedList<Future<Map<BigInteger, BigInteger>>>();
		Date start = new Date();
		System.out.println("Start at: " + start);
		// split task for building hash table to number cpu and submit each part
		// in separate thread
		for (int i = 0; i < threadCnt; i++) {
			BuildHashTable calc = new BuildHashTable(tmp * i, tmp * (i + 1));
			futures.add(executor.submit(calc));
		}
		@SuppressWarnings("unchecked")
		Map<BigInteger, BigInteger>[] xx = new Map[threadCnt];
		for (int i = 0; i < xx.length; i++) {
			xx[i] = futures.poll().get();
			Date curr = new Date();
			System.out.println((curr.getTime() - start.getTime()) / 1000
					+ " second. Now: " + curr);
		}

		Queue<Future<Map.Entry<BigInteger, BigInteger>>> res = new LinkedList<Future<Map.Entry<BigInteger, BigInteger>>>();
		Date start2 = new Date();
		System.out.println("Start meet in the middle at: " + start2);
		// split task for searching in hash table to number cpu and submit each
		// part in separate thread
		for (int i = 0; i < threadCnt; i++) {
			SearchInHashTable calc = new SearchInHashTable(tmp * i, tmp
					* (i + 1), xx);
			res.add(executor.submit(calc));
		}
		Map.Entry<BigInteger, BigInteger> r = null;
		while (!res.isEmpty()) {
			r = res.poll().get();
			Date curr = new Date();
			System.out.println((curr.getTime() - start2.getTime()) / 1000
					+ " second. Now: " + curr);
			if (r != null) {
				executor.shutdownNow();
				break;
			}
		}
		BigInteger x = r.getKey().add(r.getValue());
		Date stop = new Date();
		System.out.println("Dlog: " + x);
		System.out.println("Total " + (stop.getTime() - start.getTime()) / 1000
				+ " second. Now: " + stop);
	}
}
