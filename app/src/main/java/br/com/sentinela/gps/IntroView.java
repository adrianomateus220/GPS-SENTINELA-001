package br.com.sentinela.gps;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
/** Lightweight native cinematic, independent of location initialization. */
public final class IntroView extends View {
 private final Paint p=new Paint(3);private final Path path=new Path();private final long start=android.os.SystemClock.uptimeMillis();private Bitmap landscape;private final Drawable emblem;private final RectF target=new RectF();
 public IntroView(Context c){super(c);setContentDescription("Abertura Sentinela: conexões luminosas entre montanhas e neblina");try(java.io.InputStream in=c.getAssets().open("strands-landscape.jpg")){landscape=BitmapFactory.decodeStream(in);}catch(Exception ignored){}emblem=c.getDrawable(R.drawable.ic_sentinela);}
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),t=Math.min(3.2f,(android.os.SystemClock.uptimeMillis()-start)/1000f);c.drawColor(0xff081220);
  if(landscape!=null){float scale=Math.max(w/landscape.getWidth(),h/landscape.getHeight())*(1.05f-.012f*t),bw=landscape.getWidth()*scale,bh=landscape.getHeight()*scale;target.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);p.setAlpha(185);c.drawBitmap(landscape,null,target,p);p.setAlpha(255);}
  p.setColor(0x66081220);c.drawRect(0,0,w,h,p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.6f));for(int i=0;i<14;i++){float x=w*(i+1)/15f,len=h*(.2f+.5f*((i*7)%11)/11f),progress=Math.min(1,Math.max(0,(t-i*.045f)/1.4f));p.setColor(i%4==0?0x99facc15:0x557dd3ec);path.reset();path.moveTo(x,0);path.cubicTo(x+w*.02f, len*.3f*progress,x-w*.025f,len*.7f*progress,x,len*progress);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setColor(i%4==0?0xfffacc15:0xffbce8ef);c.drawCircle(x,len*progress,dp(1.4f),p);p.setStyle(Paint.Style.STROKE);}
  p.setStyle(Paint.Style.FILL);float reveal=Math.min(1,Math.max(0,(t-.45f)/.75f)),size=Math.min(w*.46f,dp(185)),cy=h*.46f;
  if(emblem!=null){emblem.setAlpha((int)(255*reveal));emblem.setBounds((int)((w-size)/2),(int)(cy-size),(int)((w+size)/2),(int)cy);emblem.draw(c);}
  p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.argb((int)(255*reveal),231,228,218));p.setTextSize(Math.min(dp(26),w/14));c.drawText("S E N T I N E L A",w/2,cy+dp(38),p);p.setTextSize(dp(9));c.drawText("GPS   /   CONEXÕES EM MOVIMENTO",w/2,cy+dp(60),p);
  p.setColor(0xff6c8d9e);p.setStrokeWidth(dp(1));float left=w*.3f,right=w*.7f,bar=cy+dp(92);c.drawLine(left,bar,right,bar,p);p.setColor(0xfffacc15);c.drawLine(left,bar,left+(right-left)*Math.min(1,t/2.8f),bar,p);p.setTextSize(dp(8));p.setColor(0xffc2d3df);c.drawText(t<1.7f?"CONECTADO AO SEU CAMINHO":"PRESENÇA. ORIENTAÇÃO. MOVIMENTO.",w/2,bar+dp(23),p);
  if(t>2.85f){p.setColor(Color.argb((int)(255*Math.min(1,(t-2.85f)/.35f)),8,18,32));c.drawRect(0,0,w,h,p);}if(t<3.2f)postInvalidateOnAnimation();
 }
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(landscape!=null){landscape.recycle();landscape=null;}}
}
