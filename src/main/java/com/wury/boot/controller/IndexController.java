package com.wury.boot.controller;

import com.wury.boot.form.CommentForm;
import com.wury.boot.form.UserBlogForm;
import com.wury.boot.model.*;
import com.wury.boot.repository.UserBlogRoleRepository;
import com.wury.boot.service.api.CommentService;
import com.wury.boot.service.api.PostService;
import com.wury.boot.service.api.UserBlogService;
import com.wury.boot.validator.CommentFormValidator;
import com.wury.boot.validator.UserBlogFormValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Created by WURI on 16/03/2016.
 */
@Controller
public class IndexController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

    /**
     * constant for this class
     */
    private static final Integer PAGE_SIZE_POST_LIST = 3;
    private static final String BASE_URL_POST_ALL = "/home/page/";
    private static final String SESSION_PAGED_LIST_HOLDER_POSTS = "postList";

    @Autowired
    private PostService postService;

    @Autowired
    private UserBlogService userBlogService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserBlogRoleRepository userBlogRoleRepository;

    @Autowired
    private UserBlogFormValidator userBlogFormValidator;

    @Autowired
    private CommentFormValidator commentFormValidator;

//    @InitBinder(value = "userBlogForm")
//    public void initUserBlogBinder(WebDataBinder binder){
//        binder.addValidators(userBlogFormValidator);
//    }

    @RequestMapping(value = {"/", "/home"})
    public String postRedirect(HttpServletRequest request){
        request.getSession().setAttribute(SESSION_PAGED_LIST_HOLDER_POSTS, null);
        return "redirect:/home/page/1";
    }

    @RequestMapping(value = "/home/page/{pageNumber}", method = RequestMethod.GET)
    public ModelAndView index(HttpServletRequest request, @PathVariable("pageNumber") Integer pageNumber){
        ModelAndView mav = new ModelAndView("index");
        LOGGER.debug("Logger inside IndexController");
        PagedListHolder<PostModel> pagedListPost = (PagedListHolder<PostModel>) request.getSession().getAttribute(SESSION_PAGED_LIST_HOLDER_POSTS);
        if(pagedListPost == null){
            List<PostModel> listPost = postService.findAll();
            for(PostModel pm:listPost){
                if(pm.getDeletedAt() != null){
                    continue;
                }
            }
            pagedListPost = new PagedListHolder<PostModel>(listPost);
            pagedListPost.setPageSize(PAGE_SIZE_POST_LIST);
            pagedListPost.setSort(new MutableSortDefinition("createdAt", true, true));
        }else{
            final int goToPage = pageNumber - 1;
            if (goToPage <= pagedListPost.getPageCount() && goToPage >= 0) {
                pagedListPost.setPage(goToPage);
            }
        }

        request.getSession().setAttribute(SESSION_PAGED_LIST_HOLDER_POSTS, pagedListPost);

        mav.addObject("pager", currentPage(pagedListPost));
        mav.addObject("posts", pagedListPost);
        return mav;
    }

    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String registration(@ModelAttribute UserBlogForm form, Model model){
        model.addAttribute("userBlogForm", form);
        return "registration";
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public String registrationFinish(@Valid @ModelAttribute("userBlogForm") UserBlogForm form,
                                     @RequestParam("picture") MultipartFile pictureFile,
                                     BindingResult result, RedirectAttributes redirectAttributes){
        LOGGER.debug("EXECUTE REGISTRATION");

        userBlogFormValidator.validate(form, result);

        if(result.hasErrors()){
            return "registration";
        }
        if(!pictureFile.isEmpty()){
            String newPictureName = form.getEmail();
            String pictureLocation = "src/main/webapp/images/author_file/"+newPictureName+".jpg";
            String pictureLocationValue = "/images/author_file/"+newPictureName+".jpg";
            try{
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(pictureLocation)));
                FileCopyUtils.copy(pictureFile.getInputStream(), outputStream);
                outputStream.close();

            }catch (Exception ex){
                LOGGER.debug("ERROR UPLOAD IMAGE ["+ex.getMessage()+"]");
            }

            form.setPictureLocation(pictureLocationValue);
            UserBlogModel userBlogModel = userBlogService.create(form);
            List<UserBlogRoleModel> listRoleModel = Arrays.asList(new UserBlogRoleModel(UserRole.AUTHOR.name(), userBlogModel,new Date(), userBlogModel.getId(), new Date(), userBlogModel.getId()));
            for(UserBlogRoleModel roleUser:listRoleModel){
                userBlogRoleRepository.save(roleUser);
            }
            redirectAttributes.addFlashAttribute("messageSuccessRegistration", "You have successfully registration..");
        }

        return "redirect:/signin";
    }

    @RequestMapping(value = "/author_profile/{id}", method = RequestMethod.GET)
    public ModelAndView authorProfile(@PathVariable("id") UUID id){
        ModelAndView mav = new ModelAndView("author_detail");
        UserBlogModel userBlogModel = userBlogService.findOne(id).get();
        LOGGER.debug("AUTHOR PROFILE -> "+userBlogModel.getName());
        mav.addObject("author", userBlogModel);
        return mav;
    }

    /**
     * @param id
     * @param commentForm
     * @return
     */
    @RequestMapping(value = "/post_detail/{id}", method = RequestMethod.GET)
    public ModelAndView postDetail(@PathVariable("id") UUID id, @ModelAttribute CommentForm commentForm){

        LOGGER.debug("POST DETAIL = "+id);

        ModelAndView mav = new ModelAndView("post_detail");
        PostModel postModel = postService.findOne(id).get();
        List<CommentModel> listComment = commentService.findByPostModel(postModel);
        int commentSize = listComment.size();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("post", postModel);
        map.put("commentForm", commentForm);
        map.put("comments", listComment);
        map.put("commentSize", commentSize);
        mav.addAllObjects(map);
        return mav;
    }

    /**
     *
     * @param form
     * @param result
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "/create_comment", method = RequestMethod.POST)
    public String createComment(@Valid @ModelAttribute("commentForm") CommentForm form,
                                BindingResult result, RedirectAttributes redirectAttributes){
        commentFormValidator.validate(form, result);
        if(result.hasErrors()){
            return "redirect:/post_detail/"+form.getPostModel();
        }
        //messageSuccessCreateComment
        commentService.createComment(form);
        redirectAttributes.addFlashAttribute("messageSuccessCreateComment", "You have successfully create a comment..");
        return "redirect:/post_detail/"+form.getPostModel();
    }

    private PagerModel currentPage(PagedListHolder<?> pagedListHolder) {
        int currentIndex = pagedListHolder.getPage() + 1;
        int beginIndex = Math.max(1, currentIndex - 5);
        int endIndex = Math.min(beginIndex + 5, pagedListHolder.getPageCount());
        int totalPageCount = pagedListHolder.getPageCount();
        int totalItems = pagedListHolder.getNrOfElements();
        String baseUrl = BASE_URL_POST_ALL;

        PagerModel pager = new PagerModel();
        pager.setBeginIndex(beginIndex);
        pager.setEndIndex(endIndex);
        pager.setCurrentIndex(currentIndex);
        pager.setTotalPageCount(totalPageCount);
        pager.setTotalItems(totalItems);
        pager.setBaseUrl(baseUrl);
        return pager;
    }
}
