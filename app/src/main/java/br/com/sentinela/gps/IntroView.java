package br.com.sentinela.gps;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Native layered cinematic; no network or work on the location executor. */
public final class IntroView extends View {
 public static final int DURATION_MS=4800;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Path path=new Path();private final RectF rect=new RectF();
 private final long start=android.os.SystemClock.uptimeMillis();private Bitmap landscape;
 private final Typeface light=Typeface.create("sans-serif-light",0),medium=Typeface.create("sans-serif-medium",0);
 private LinearGradient shade;private RadialGradient mist;private final Matrix mistMatrix=new Matrix();
 public IntroView(Context c){super(c);setContentDescription("Abertura Sentinela: vale entre montanhas, névoa e conexões luminosas");try(java.io.InputStream in=c.getAssets().open("strands-landscape.jpg")){landscape=BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}}
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 private float ease(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){shade=new LinearGradient(0,0,0,h,new int[]{0xc0051019,0x10051019,0x50051019,0xff050d15},new float[]{0,.35f,.58f,1},Shader.TileMode.CLAMP);mist=new RadialGradient(0,0,Math.max(1,w*.75f),new int[]{0x287ec3d3,0x007ec3d3},null,Shader.TileMode.CLAMP);}
 private void label(Canvas c,String text,float x,float y,float size,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(light);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(text,x,y,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),t=(android.os.SystemClock.uptimeMillis()-start)/1000f;c.drawColor(0xff08121c);
  if(landscape!=null){float scale=Math.max(w/landscape.getWidth(),h/landscape.getHeight())*(1.13f-.015f*Math.min(t,4.8f)),bw=landscape.getWidth()*scale,bh=landscape.getHeight()*scale;float drift=(float)Math.sin(t*.23f)*w*.025f;rect.set((w-bw)/2+drift,(h-bh)/2,(w+bw)/2+drift,(h+bh)/2);p.setColor(Color.WHITE);c.drawBitmap(landscape,null,rect,p);}
  p.setShader(shade);c.drawRect(0,0,w,h,p);p.setShader(null);
  // Distinct fog layers glide over the landscape without bitmap allocations.
  for(int i=0;i<3;i++){mistMatrix.setScale(1.5f,.35f);mistMatrix.postTranslate(w*(.1f+i*.4f)+(float)Math.sin(t*.32f+i)*w*.15f,h*(.36f+i*.13f));mist.setLocalMatrix(mistMatrix);p.setShader(mist);c.drawRect(0,0,w,h,p);}p.setShader(null);
  float cx=w*.5f,cy=h*.39f,r=Math.min(w*.17f,dp(68)),reveal=ease((t-.3f)/1.6f);
  // Fine luminous strands converge into a purpose-built navigation crest.
  for(int i=0;i<18;i++){float x=w*(i+.5f)/18f,end=cy-r*.4f,progress=ease((t-i*.025f)/1.6f);path.reset();path.moveTo(x,-10);path.cubicTo(x,h*.2f,cx+(x-cx)*.4f,end*.85f,cx+(x-cx)*.17f,end);c.save();c.clipRect(0,0,w,end*progress);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(i%5==0?.9f:.45f));p.setColor(i%5==0?0x99d7c08a:0x5577bbc9);c.drawPath(path,p);c.restore();}
  c.save();c.translate(cx,cy);p.setStyle(Paint.Style.STROKE);p.setColor(Color.argb((int)(145*reveal),140,206,218));p.setStrokeWidth(dp(.65f));rect.set(-r,-r,r,r);c.drawArc(rect,-90,360*reveal,false,p);rect.inset(dp(7),dp(7));c.drawArc(rect,t*9,110,false,p);c.drawArc(rect,180+t*9,110,false,p);
  for(int i=0;i<48;i++){double angle=i*Math.PI/24;float ra=r+dp(5),rb=ra+dp(i%4==0?5:2);p.setColor(i%4==0?0x99d6c393:0x44618b9c);c.drawLine((float)Math.cos(angle)*ra,(float)Math.sin(angle)*ra,(float)Math.cos(angle)*rb,(float)Math.sin(angle)*rb,p);}
  c.scale(.85f+.15f*reveal,.85f+.15f*reveal);path.reset();path.moveTo(0,-r*.68f);path.lineTo(r*.34f,r*.44f);path.lineTo(0,r*.20f);path.lineTo(-r*.34f,r*.44f);path.close();p.setStyle(Paint.Style.FILL);p.setColor(Color.argb((int)(255*reveal),221,207,164));c.drawPath(path,p);p.setColor(0xff132733);path.reset();path.moveTo(0,-r*.38f);path.lineTo(0,r*.13f);path.lineTo(-r*.19f,r*.27f);path.close();c.drawPath(path,p);c.restore();
  float title=ease((t-1.05f)/1.1f);int alpha=(int)(255*title);float y=h*.60f+dp(10)*(1-title);String name="SENTINELA";float font=Math.min(dp(36),w*.083f),spacing=dp(4)+(1-title)*dp(9);p.setTypeface(light);p.setTextSize(font);float total=p.measureText(name)+spacing*(name.length()-1),x=(w-total)/2;p.setTextAlign(Paint.Align.LEFT);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(alpha,240,240,229));for(int i=0;i<name.length();i++){String letter=name.substring(i,i+1);c.drawText(letter,x,y,p);x+=p.measureText(letter)+spacing;}
  label(c,"C O N E C T A D O   A O   S E U   C A M I N H O",cx,y+dp(27),Math.min(dp(8),w/49),Color.argb(alpha,170,200,208));
  float baseline=h*.79f;p.setColor(0x44658090);p.setStrokeWidth(dp(.7f));c.drawLine(w*.18f,baseline,w*.82f,baseline,p);p.setColor(0xffd6c393);float progress=ease(t/4.3f);c.drawLine(w*.18f,baseline,w*(.18f+.64f*progress),baseline,p);p.setStyle(Paint.Style.FILL);c.drawCircle(w*(.18f+.64f*progress),baseline,dp(2),p);
  label(c,t<1.5f?"CADA CAMINHO, UMA CONEXÃO":t<3?"PRESENÇA EM MOVIMENTO":"SEU CAMINHO COMEÇA AQUI",cx,baseline+dp(24),dp(9),0xffafc6cf);
  label(c,"G P S   /   S E N T I N E L A",cx,h*.12f,dp(8),0xffbdd1d7);
  if(t<.55f||t>4.25f){float fade=t<.55f?1-ease(t/.55f):ease((t-4.25f)/.55f);p.setColor(Color.argb((int)(255*fade),5,13,21));c.drawRect(0,0,w,h,p);}if(t<DURATION_MS/1000f)postInvalidateOnAnimation();
 }
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(landscape!=null){landscape.recycle();landscape=null;}}
}
