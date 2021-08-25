package net.csibio.propro.domain.vo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class LibraryUpdateVO {
    String id;
    String name;
    String type;
    String organism;
    Set<String> tags;
    String description;
    MultipartFile libFile;
}
