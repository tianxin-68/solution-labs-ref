package com.bi.queryer.ssm.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TextToZipUtil {

    /**
     * 将 CSV 字符串内容压缩成 ZIP 字节数组
     *
     * @param txtContent CSV 完整内容
     * @param fileName ZIP 里的 csv 文件名，如 data.csv
     * @return zip 字节数组，可直接上传/写入文件
     * @throws IOException
     */
    public static byte[] toZip(String txtContent, String fileName) throws IOException {
        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(zipBaos)) {
            // 创建 ZIP 内部文件条目
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            // 将 CSV 内容写入 ZIP
            try (ByteArrayInputStream csvIs = new ByteArrayInputStream(txtContent.getBytes(StandardCharsets.UTF_8))) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = csvIs.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
            }

            zos.closeEntry();
            zos.finish();
        }

        return zipBaos.toByteArray();
    }
}
