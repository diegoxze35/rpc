package com.net.grpcfile.service;

import com.google.protobuf.ByteString;
import file.service.proto.GetFileRequest;
import file.service.proto.GetFileResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Semaphore;
import org.springframework.grpc.server.service.GrpcService;
import static file.service.proto.FileServiceGrpc.FileServiceImplBase;

@GrpcService
public class FileServiceImpl extends FileServiceImplBase {

	private static final int CHUNK_SIZE = 4 * 1024 * 1024;
	private final Semaphore concurrencyLimiter = new Semaphore(10);

	@Override
	public void getFile(GetFileRequest request, StreamObserver<GetFileResponse> responseObserver) {
		if (!concurrencyLimiter.tryAcquire()) {
			responseObserver.onError(Status.RESOURCE_EXHAUSTED
					.withDescription("Max clients")
					.asException());
			return;
		}
		try (
				RandomAccessFile file = new RandomAccessFile(request.getFileName(), "r");
				FileChannel channel = file.getChannel()
		) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(CHUNK_SIZE);
			while (channel.read(buffer) > 0) {
				buffer.flip();
				GetFileResponse response = GetFileResponse.newBuilder()
						.setData(ByteString.copyFrom(buffer))
						.build();
				responseObserver.onNext(response);
				buffer.clear();
			}
			responseObserver.onCompleted();
		} catch (IOException e) {
			Status status = e instanceof FileNotFoundException ? Status.NOT_FOUND : Status.INTERNAL;
			responseObserver.onError(status.withDescription(e.getMessage()).asException());
		} finally {
			concurrencyLimiter.release();
		}
	}
}
