package com.xlong.xrpc.protocol;

import com.xlong.xrpc.util.SerializationUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class XProtobufDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(XProtobufDecoder.class);

    private Class<?> genericClass;

    public XProtobufDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in,
                          List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        logger.info("Server receive msg length {}", dataLength);
        Object obj = SerializationUtils.deserialize(data, genericClass);
        out.add(obj);
    }
}
