package com.vm.service.impl;

import com.vm.dao.mapper.VmFilesMapper;
import com.vm.dao.mapper.VmUsersMapper;
import com.vm.dao.po.*;
import com.vm.dao.qo.VmMoviesQueryBean;
import com.vm.service.base.BaseService;
import com.vm.service.inf.VmUsersService;
import com.vm.utils.DateUtil;
import com.vm.utils.VmProperties;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by ZhangKe on 2017/12/28.
 */
@Service
public class VmUsersServiceImpl extends BaseService implements VmUsersService {
    @Autowired
    private VmUsersMapper vmUsersMapper;

    @Autowired
    private VmFilesMapper vmFilesMapper;


    /**
     * 通过username获取user
     *
     * @param username
     * @return
     */
    private VmUsers getUserByUsername(String username) {
        //是否存在此username的user
        VmUsersExample example = new VmUsersExample();
        VmUsersExample.Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        criteria.andStatusEqualTo(BasePo.Status.NORMAL.getCode());
        List<VmUsers> vmUsers = vmUsersMapper.selectByExample(example);
        if (isEmptyList(vmUsers)) {
            return null;
        }
        return vmUsers.get(0);
    }

    @Override
    public VmUsers userLogin(CustomVmUsers user) throws Exception {


        //user是否存在
        VmUsers dbUser = getUserByUsername(user.getUsername());
        eject(isNullObject(dbUser),
                "userLogin dbUser is not exits ! user is :" + dbUser);

        //密码错误
        eject(!dbUser.getPassword().equals(user.getPassword()),
                "userLogin password is error ! user is :" + user);


        return dbUser;
    }

    @Override
    public VmUsers getUserBasicInfo(Long userId) {
        eject(isNullObject(userId),
                "getUserBasicInfo userId is null! userId is:" + userId);

        //获取指定id的user

        VmUsers dbUser = vmUsersMapper.selectByPrimaryKey(userId);

        eject(isNullObject(dbUser) || BasePo.Status.isDeleted(dbUser.getStatus()),
                "getUserBasicInfo user is not exits! userId is:" + userId);

        //屏蔽相关信息
        coverUserSomeInfo(dbUser);

        return dbUser;
    }

    private void coverUserSomeInfo(VmUsers dbUser) {
        dbUser.setPassword("");
    }

    @Override
    public void updateUserBasicInfo(CustomVmUsers user) throws Exception {
        //user是否存在
        VmUsers dbUser = getUserByUsername(user.getUsername());
        eject(isNullObject(dbUser),
                "updateUserBasicInfo dbUser is not exits ! user is :" + dbUser);

        vmUsersMapper.updateByPrimaryKeySelective(makeVmUsers(user));
    }

    @Override
    public void sendUserImg(Long userId, VmMoviesQueryBean query, HttpServletResponse response) throws Exception {
        FileInputStream input = null;
        ServletOutputStream output = null;
        try {
            //获取用户图片id信息
            VmFiles file = vmFilesMapper.selectByPrimaryKey(userId);
            String userImgPath = VmProperties.VM_USER_IMG_PATH;
            String width = query.getImgWidth();
            String userImgName = null;
            String contentType = null;
            if (file != null) {
                contentType = file.getContentType();
                userImgName = file.getFilename();
            }
            File f = new File(userImgPath + File.separator + width + "_" + userImgName);
            //不存在，返回默认图片
            if (!f.exists()) {
                width = VmProperties.VM_USER_IMG_DEFAULT_WIDTH;
                userImgName = VmProperties.VM_USER_IMG_DEFAULT_NAME;
                f = new File(userImgPath + File.separator + width + "_" + userImgName);
            }
            output = response.getOutputStream();
            input = new FileInputStream(f);
            //设置响应的媒体类型

            response.setContentType(contentType); // 设置返回的文件类型
            IOUtils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(input, output);
        }
    }

    /**
     * 构建VmUsers
     *
     * @param user
     * @return
     */
    private VmUsers makeVmUsers(CustomVmUsers user) {
        VmUsers vmUser = new VmUsers();
        vmUser.setBirthday(user.getBirthday());
        vmUser.setUpdateTime(DateUtil.unixTime().intValue());
        vmUser.setDescription(user.getDescription());
        vmUser.setSex(user.getSex());
        vmUser.setUsername(user.getUsername());
        return vmUser;
    }

}