package com.net.grpcfile.service;

import com.google.protobuf.ByteString;
import file.service.proto.GetFileRequest;
import file.service.proto.GetFileResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
/*import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;*/
import java.io.IOException;
//import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import static file.service.proto.FileServiceGrpc.FileServiceImplBase;

/*@Service
public class FileServiceImpl extends FileServiceImplBase {

	private static final int CHUNK_SIZE = 512 * 1024;

	@Override
	public void getFile(GetFileRequest request, StreamObserver<GetFileResponse> responseObserver) {
		File file;
		System.out.println(request.getFileName());
		try {
			file = ResourceUtils.getFile("classpath:" + request.getFileName());
		} catch (FileNotFoundException e) {
			responseObserver.onError(Status.NOT_FOUND
					.withDescription("File not found")
					.withCause(e)
					.asException());
			return;
		}

		try (InputStream fis = new FileInputStream(file)*//*;
		     ByteArrayOutputStream bos = new ByteArrayOutputStream(CHUNK_SIZE)*//*) {
			byte[] buffer = new byte[CHUNK_SIZE];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				*//*bos.write(buffer, 0, bytesRead);
				byte[] fileData = bos.toByteArray();*//*
				GetFileResponse response = GetFileResponse.newBuilder()
						//.setData(ByteString.copyFrom(fileData))
						.setData(ByteString.copyFrom(buffer, 0, bytesRead))
						.build();
				responseObserver.onNext(response);
			}
			responseObserver.onCompleted();
		} catch (IOException e) {
			responseObserver.onError(Status.INTERNAL
					.withDescription("Error reading file")
					.withCause(e)
					.asException());
		}
	}
}*/

@Service
public class FileServiceImpl extends FileServiceImplBase {

    private static final int CHUNK_SIZE = 64 * 1024;  // 64 KB
    private final Semaphore concurrencyLimiter = new Semaphore(20);

    @Override
    public void getFile(GetFileRequest request, StreamObserver<GetFileResponse> responseObserver) {
        if (!concurrencyLimiter.tryAcquire()) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED
                .withDescription("Max clients")
                .asException());
            return;
        }

        try {
            Path filePath = Paths.get(ResourceUtils.getFile("classpath:" + request.getFileName()).toURI());

            try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(CHUNK_SIZE);

                while (fileChannel.read(buffer) != -1) {
                    buffer.flip();

                    GetFileResponse response = GetFileResponse.newBuilder()
                        .setData(ByteString.copyFrom(buffer))
                        .build();

                    responseObserver.onNext(response);
                    buffer.clear();
                }

                responseObserver.onCompleted();
            } catch (IOException e) {
                responseObserver.onError(Status.INTERNAL
                    .withDescription("Error reading file")
                    .withCause(e)
                    .asException());
            }
        } catch (Exception e) {
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("File not found")
                .withCause(e)
                .asException());
        } finally {
            concurrencyLimiter.release();
        }
    }
}


