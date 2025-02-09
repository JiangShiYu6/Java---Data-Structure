package com.itheima;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Student {
    Integer sid;
    String name;
    Integer grade;
}
