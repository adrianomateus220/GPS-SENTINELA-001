package br.com.sentinela.gps;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/** At most one running task and one pending value; newest value wins. */
public final class LatestTask<T> {
 private final Executor executor;private final Consumer<T> action;private T pending;private boolean running;
 public LatestTask(Executor e,Consumer<T> a){executor=e;action=a;}
 public synchronized void submit(T value){pending=value;if(!running){running=true;executor.execute(this::drain);}}
 public synchronized void clear(){pending=null;}
 private void drain(){for(;;){T next;synchronized(this){next=pending;pending=null;if(next==null){running=false;return;}}try{action.accept(next);}catch(RuntimeException ignored){/* one malformed fix must not strand the queue */}}}
}
