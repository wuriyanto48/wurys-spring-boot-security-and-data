package com.wury.boot.repository;

import com.wury.boot.model.UserBlogRole;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Created by WURI on 16/03/2016.
 */
@Repository
public interface UserBlogRoleRepository extends CrudRepository<UserBlogRole, UUID>{

}
