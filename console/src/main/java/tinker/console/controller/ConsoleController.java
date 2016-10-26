package tinker.console.controller;

import org.apache.tomcat.jni.File;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import tinker.console.common.BizAssert;
import tinker.console.common.BizException;
import tinker.console.common.Constants;
import tinker.console.common.RestResponse;
import tinker.console.domain.AppInfo;
import tinker.console.domain.BasicUser;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import tinker.console.domain.PatchInfo;
import tinker.console.domain.VersionInfo;
import tinker.console.service.AccountService;
import tinker.console.service.AppService;
import tinker.console.service.PatchService;
import tinker.console.utils.HttpRequestUtils;
import java.util.List;

/**
 * Created by tong on 15/10/24.
 */
@Controller
public class ConsoleController {
    @Autowired
    private AppService appService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PatchService patchService;

    @RequestMapping(value = "/console",method = RequestMethod.GET)
    public ModelAndView index(HttpServletRequest req) {
        RestResponse restR = new RestResponse();

        BasicUser basicUser = (BasicUser) req.getSession().getAttribute(Constants.SESSION_LOGIN_USER);
        List<AppInfo> appInfoList = appService.findAll(basicUser);
        restR.getData().put("user",basicUser);
        restR.getData().put("appInfoList",appInfoList);
        return new ModelAndView("console","restR",restR);
    }

    @RequestMapping(value = "/app/create",method = RequestMethod.GET)
    public ModelAndView app_create(String msg) {
        RestResponse restR = new RestResponse();
        restR.setMessage(HttpRequestUtils.urlDecode(msg));
        return new ModelAndView("create_app","restR",restR);
    }

    @RequestMapping(value = "/app/create",method = RequestMethod.POST)
    public ModelAndView app_create(HttpServletRequest req,String appname,String description) {
        try {
            BizAssert.notEpmty(appname,"应用名不能为空");
            BizAssert.notEpmty(description,"描述信息不能为空");

            BasicUser basicUser = (BasicUser) req.getSession().getAttribute(Constants.SESSION_LOGIN_USER);
            appService.addApp(basicUser,appname,description,"Android");
            return new ModelAndView("redirect:/console");
        } catch (BizException e) {
            return new ModelAndView("redirect:/app/create?msg=" + HttpRequestUtils.urlEncode(e.getMessage()));
        }
    }

    @RequestMapping(value = "/app",method = RequestMethod.GET)
    public ModelAndView app(String appUid) {
        RestResponse restR = new RestResponse();
        AppInfo appInfo = appService.findByUid(appUid);
        if (appUid == null) {
            restR.setCode(-1);
            restR.setMessage("该应用未找到");
        }
        else {
            restR.getData().put("appInfo",appInfo);
            restR.getData().put("versionList",appService.findAllVersion(appInfo));
        }
        return new ModelAndView("app","restR",restR);
    }

    @RequestMapping(value = "/app/create_version",method = RequestMethod.GET)
    public ModelAndView addVersion(String appUid,String msg) {
        RestResponse restR = new RestResponse();
        restR.setMessage(HttpRequestUtils.urlDecode(msg));
        try {
            BizAssert.notEpmty(appUid,"应用号不能为空");

            AppInfo appInfo = appService.findByUid(appUid);
            if (appInfo == null) {
                throw new BizException("此应用不存在");
            }
            restR.getData().put("appUid",appUid);
        } catch (BizException e) {
            restR.setCode(-1);
            restR.setMessage(e.getMessage());
        }
        return new ModelAndView("create_version","restR",restR);
    }

    @RequestMapping(value = "/app/create_version",method = RequestMethod.POST)
    public ModelAndView addVersion(HttpServletRequest req,String appUid,String versionName) {
        RestResponse restR = new RestResponse();
        try {
            BizAssert.notEpmty(appUid,"应用号不能为空");
            BizAssert.notEpmty(versionName,"版本号不能为空");
            restR.getData().put("appUid",appUid);

            AppInfo appInfo = appService.findByUid(appUid);
            if (appInfo == null) {
                throw new BizException("此应用不存在");
            }
            VersionInfo versionInfo = appService.findVersionByUidAndVersionName(appInfo,versionName);
            if (versionInfo != null) {
                throw new BizException("此版本已存在");
            }

            BasicUser basicUser = (BasicUser) req.getSession().getAttribute(Constants.SESSION_LOGIN_USER);
            versionInfo = new VersionInfo();
            versionInfo.setUserId(accountService.getRootUserId(basicUser));
            versionInfo.setAppUid(appUid);
            versionInfo.setVersionName(versionName);
            appService.saveVersionInfo(versionInfo);

            return new ModelAndView("redirect:/app?appUid=" + appUid);
        } catch (BizException e) {
            return new ModelAndView("redirect:/app/create_version?appUid=" + appUid + "&msg=" + HttpRequestUtils.urlEncode(e.getMessage()));
        }
    }

    @RequestMapping(value = "/app/version",method = RequestMethod.GET)
    public ModelAndView app_version(String appUid,String versionName) {
        RestResponse restR = new RestResponse();
        try {
            BizAssert.notEpmty(appUid,"应用号不能为空");
            BizAssert.notEpmty(versionName,"版本号不能为空");

            AppInfo appInfo = appService.findByUid(appUid);
            if (appUid == null) {
                throw new BizException("该应用未找到");
            }

            VersionInfo versionInfo = appService.findVersionByUidAndVersionName(appInfo,versionName);
            if (versionInfo == null) {
                throw new BizException("该版本未找到: " + versionName);
            }
            //加载所有patch信息
            List<PatchInfo> patchInfoList = patchService.findByUidAndVersionName(appUid,versionName);

            restR.getData().put("appInfo",appInfo);
            restR.getData().put("versionInfo",versionInfo);
            restR.getData().put("patchInfoList",patchInfoList);
        } catch (BizException e) {
            restR.setCode(-1);
            restR.setMessage(e.getMessage());
        }
        return new ModelAndView("version","restR",restR);
    }

    @RequestMapping(value = "/patch/create",method = RequestMethod.GET)
    public ModelAndView patch_create(String appUid,String versionName,String msg) {
        RestResponse restR = new RestResponse();
        restR.setMessage(msg);
        try {
            BizAssert.notEpmty(appUid,"应用号不能为空");
            BizAssert.notEpmty(versionName,"版本号不能为空");

            AppInfo appInfo = appService.findByUid(appUid);
            if (appUid == null) {
                throw new BizException("该应用未找到");
            }

            VersionInfo versionInfo = appService.findVersionByUidAndVersionName(appInfo,versionName);
            if (versionInfo == null) {
                throw new BizException("该版本未找到: " + versionName);
            }
            restR.getData().put("versionInfo",versionInfo);
            restR.getData().put("appInfo",appInfo);
        } catch (BizException e) {
            restR.setCode(-1);
            restR.setMessage(e.getMessage());
        }
        return new ModelAndView("create_patch","restR",restR);
    }

    @RequestMapping(value = "/patch/create",method = RequestMethod.POST)
    public ModelAndView patch_create(String appUid,String versionName,String description,@RequestParam("file") CommonsMultipartFile multipartFile) {
        RestResponse restR = new RestResponse();
        try {
            BizAssert.notEpmty(appUid,"应用号不能为空");
            BizAssert.notEpmty(versionName,"版本号不能为空");
            BizAssert.notEpmty(description,"描述不能为空");
            BizAssert.notNull(multipartFile,"请选择文件");

            AppInfo appInfo = appService.findByUid(appUid);
            if (appUid == null) {
                throw new BizException("该应用未找到");
            }

            VersionInfo versionInfo = appService.findVersionByUidAndVersionName(appInfo,versionName);
            if (versionInfo == null) {
                throw new BizException("该版本未找到: " + versionName);
            }

            patchService.savePatch(appInfo,versionInfo,description,multipartFile);

            return new ModelAndView("redirect:/app/version?appUid=" + appUid + "&versionName=" + versionName);
        } catch (BizException e) {
            restR.setCode(-1);
            restR.setMessage(e.getMessage());
            return new ModelAndView("redirect:/patch/create?appUid=" + appUid + "&msg=" + HttpRequestUtils.urlEncode(e.getMessage()));
        }
    }
}