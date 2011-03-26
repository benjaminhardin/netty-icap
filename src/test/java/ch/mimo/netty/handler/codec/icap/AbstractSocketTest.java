package ch.mimo.netty.handler.codec.icap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.internal.ExecutorUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;


public abstract class AbstractSocketTest extends Assert {
	
	private static ExecutorService executor;
	
	@BeforeClass
	public static void init() {
		executor = Executors.newCachedThreadPool();
	}
	
	@AfterClass
	public static void destroy() {
		ExecutorUtil.terminate(executor);
	}
	
    protected abstract ChannelFactory newServerSocketChannelFactory(Executor executor);
    protected abstract ChannelFactory newClientSocketChannelFactory(Executor executor);
    
    public void runDecoderTest(SimpleChannelUpstreamHandler handler, IcapMessage message) throws Exception {
        ServerBootstrap serverBootstrap  = new ServerBootstrap(newServerSocketChannelFactory(executor));
        ClientBootstrap clientBootstrap = new ClientBootstrap(newClientSocketChannelFactory(executor));
    
        serverBootstrap.getPipeline().addLast("decoder",new IcapRequestDecoder());
        serverBootstrap.getPipeline().addAfter("decoder","handler",handler);
        // TODO add client message encoder();
        //clientBootstrap.getPipeline().addLast("encoder",new IcapMessageEncoder());
        
        int port = findUsablePort();
        Channel channel = serverBootstrap.bind( new InetSocketAddress("Localhost",port));
        
        ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress("localhost",port));
        assertTrue(channelFuture.awaitUninterruptibly().isSuccess());

        Channel clientChannel = channelFuture.getChannel();
        
        clientChannel.write(message);
    }
    
    private int findUsablePort() {
    	Random rand = new Random();
    	while(true) {
    		int port = rand.nextInt(4000);
	    	try {
				Socket socket = new Socket();
				socket.bind(new InetSocketAddress("localhost",port));
				socket.close();
			} catch (UnknownHostException e) {
				// NOOP
			} catch (IOException e) {
				// NOOP
			}
			return port;
    	}
    }
/*

    private static final Random random = new Random();
    static final byte[] data = new byte[1048576];

    private static ExecutorService executor;

    static {
        random.nextBytes(data);
    }

    @BeforeClass
    public static void init() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void destroy() {
        ExecutorUtil.terminate(executor);
    }

    protected abstract ChannelFactory newServerSocketChannelFactory(Executor executor);
    protected abstract ChannelFactory newClientSocketChannelFactory(Executor executor);

    @Test
    public void testFixedLengthEcho() throws Throwable {
        ServerBootstrap sb = new ServerBootstrap(newServerSocketChannelFactory(executor));
        ClientBootstrap cb = new ClientBootstrap(newClientSocketChannelFactory(executor));

        EchoHandler sh = new EchoHandler();
        EchoHandler ch = new EchoHandler();

        sb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(1024));
        sb.getPipeline().addAfter("decoder", "handler", sh);
        cb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(1024));
        cb.getPipeline().addAfter("decoder", "handler", ch);

        Channel sc = sb.bind(new InetSocketAddress(0));
        int port = ((InetSocketAddress) sc.getLocalAddress()).getPort();

        ChannelFuture ccf = cb.connect(new InetSocketAddress(TestUtil.getLocalHost(), port));
        assertTrue(ccf.awaitUninterruptibly().isSuccess());

        Channel cc = ccf.getChannel();
        for (int i = 0; i < data.length;) {
            int length = Math.min(random.nextInt(1024 * 3), data.length - i);
            cc.write(ChannelBuffers.wrappedBuffer(data, i, length));
            i += length;
        }

        while (ch.counter < data.length) {
            if (sh.exception.get() != null) {
                break;
            }
            if (ch.exception.get() != null) {
                break;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }

        while (sh.counter < data.length) {
            if (sh.exception.get() != null) {
                break;
            }
            if (ch.exception.get() != null) {
                break;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }

        sh.channel.close().awaitUninterruptibly();
        ch.channel.close().awaitUninterruptibly();
        sc.close().awaitUninterruptibly();

        if (sh.exception.get() != null && !(sh.exception.get() instanceof IOException)) {
            throw sh.exception.get();
        }
        if (ch.exception.get() != null && !(ch.exception.get() instanceof IOException)) {
            throw ch.exception.get();
        }
        if (sh.exception.get() != null) {
            throw sh.exception.get();
        }
        if (ch.exception.get() != null) {
            throw ch.exception.get();
        }
    }

    private class EchoHandler extends SimpleChannelUpstreamHandler {
        volatile Channel channel;
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        volatile int counter;

        EchoHandler() {
            super();
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
                throws Exception {
            channel = e.getChannel();
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {
            ChannelBuffer m = (ChannelBuffer) e.getMessage();
            assertEquals(1024, m.readableBytes());

            byte[] actual = new byte[m.readableBytes()];
            m.getBytes(0, actual);

            int lastIdx = counter;
            for (int i = 0; i < actual.length; i ++) {
                assertEquals(data[i + lastIdx], actual[i]);
            }

            if (channel.getParent() != null) {
                channel.write(m);
            }

            counter += actual.length;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            if (exception.compareAndSet(null, e.getCause())) {
                e.getChannel().close();
            }
        }
    }
 */
}
