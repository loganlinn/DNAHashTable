public class Link<E> {
	private E element;
	private Link<E> next;

	public Link(E element, Link<E> next) {
		this.element = element;
		this.next = next;
	}
	
	public Link(Link<E> next){
		this.next = next;
	}
	
	public Link<E> next(){
		return next;
	}
	
	public E element() {
		return element;
	}
	
	public E setElement(E element){
		return this.element = element;
	}
	
	public Link<E> setNext(Link<E> next){
		return this.next = next;
	}
}
