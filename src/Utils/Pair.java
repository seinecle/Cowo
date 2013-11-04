/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;


/**
 *
 * @author C. Levallois retrieved from
 * http://stackoverflow.com/questions/521171/a-java-collection-of-value-pairs-tuples
 */
public class Pair<L extends Comparable<? super L>, R extends Comparable<? super R>> implements Comparable<Pair<L,R>> {

    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair pairo = (Pair) o;
        return this.left.equals(pairo.getLeft())
                && this.right.equals(pairo.getRight());
    }

    @Override
    public int compareTo(Pair<L,R> other) {
        int cmp = this.left.compareTo(other.left);
        if(cmp==0) {
            cmp = this.right.compareTo(other.right);
        }
        return cmp;
    }
}