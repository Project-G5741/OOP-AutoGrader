package com.eiu.capstone.backend.service.bulk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * MultipartFile wrapper that only rewrites {@link #getOriginalFilename()} for path remap.
 */
final class RenamedMultipartFile implements MultipartFile {

    private final MultipartFile delegate;
    private final String originalFilename;

    RenamedMultipartFile(MultipartFile delegate, String originalFilename) {
        this.delegate = delegate;
        this.originalFilename = originalFilename;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return delegate.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        delegate.transferTo(dest);
    }
}
