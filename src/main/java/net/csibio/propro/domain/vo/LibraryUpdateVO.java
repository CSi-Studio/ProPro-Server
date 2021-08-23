package net.csibio.propro.domain.vo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LibraryUpdateVO {
    String id;
    String name;
    String type;
    String organism;
    String description;
    MultipartFile libFile;
}
