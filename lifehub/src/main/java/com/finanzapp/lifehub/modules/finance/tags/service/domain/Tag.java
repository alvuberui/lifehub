package com.finanzapp.tags.service.domain;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag {

    private Long id;
    private String name;
    private String color;
}
