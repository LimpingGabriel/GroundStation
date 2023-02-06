package application.map;

public class TileKey {
	private int tx;
	private int ty;
	public TileKey(int tx, int ty) {
		this.tx = tx;
		this.ty = ty;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("%d, %d", this.tx, this.ty);
	}
	
	@Override
	public boolean equals(Object key) {
		return this.toString().equals(key.toString());
	}
}