package com.itheima;

import com.itheima.entity.UserEntity;
import com.itheima.Mapper.Test;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner= new Scanner(System.in);
        String input = scanner.nextLine();
        System.out.println("您输入的是：" + input);

        scanner.close(); // 关闭 Scanner 对象

    }
}
