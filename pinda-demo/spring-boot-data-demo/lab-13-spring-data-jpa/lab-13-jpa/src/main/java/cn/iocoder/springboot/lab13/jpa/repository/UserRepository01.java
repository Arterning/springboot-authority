package cn.iocoder.springboot.lab13.jpa.repository;

import cn.iocoder.springboot.lab13.jpa.dataobject.UserDO;
import org.springframework.data.repository.CrudRepository;

/**
CrudRepository<Entity,主键类型>
Integer会不会不够用？
 */
public interface UserRepository01 extends CrudRepository<UserDO, Integer> {

}
