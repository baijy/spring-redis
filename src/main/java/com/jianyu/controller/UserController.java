package com.jianyu.controller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.jianyu.modle.User;

@Controller
@RequestMapping("user")
public class UserController {

	@Autowired
	public RedisTemplate<String, String> redisTemplate;
	public ZSetOperations<String, String> zsetOps;
	public HashOperations<String, String, Object> zhashOps;

	public static final String REDIS_KEY_USER_IDS = "user_ids";

	public static final String REDIS_KEY_USER = "user_%s";

	@PostConstruct
	public void init() {
		zsetOps = redisTemplate.opsForZSet();
		zhashOps = redisTemplate.opsForHash();
		
	}
	
	/**
	 * 分页查询redis的数据
	 * @param p
	 * @param model
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String index(@RequestParam(value = "p", required = false) Integer p,
			ModelMap model) throws UnsupportedEncodingException {
		if (p == null || p <= 0) {
			p = 1;
		}
		// 计算总数，分页
		int total = zsetOps.zCard(REDIS_KEY_USER_IDS).intValue();
		int pageSize = 5;
		int pageTotal = total / pageSize + (total % pageSize > 0 ? 1 : 0);
		if (pageTotal > 0 && p > pageTotal) {
			p = pageTotal;
		}
		int start = (p - 1) * pageSize;
		int end = p * pageSize - 1;

		// 按创建时间倒序排序
		Set<TypedTuple<String>> _ids = zsetOps.reverseRangeWithScores(
				REDIS_KEY_USER_IDS, start, end);
		
		// 查询和组装详情
		List<User> users = new ArrayList<User>();
		Iterator<TypedTuple<String>> iterator = _ids.iterator();
		while (iterator.hasNext()) {
			TypedTuple<String> _id = iterator.next();
			String id = _id.getValue();
			String userKey = String.format(REDIS_KEY_USER, id);
			String createTime = zhashOps.get(userKey, User.PRO_CREATETIME).toString();
			String name = new String(zhashOps.get(userKey, User.PRO_NAME).toString().getBytes("iso8859-1"), "utf-8");
			User user = new User();
			user.setCreateTime(createTime);
			user.setName(name);
			user.setId(id);
			users.add(user);
		}
		model.put("users", users);
		model.put("total", total);
		model.put("p", p);
		model.put("page_total", pageTotal);
		return "user/index";
	}
	
	/**
	 * 跳转到添加用户页面
	 * @param model
	 */
	@RequestMapping(value = "create", method = RequestMethod.GET)
	public void create(ModelMap model) {
		User user = new User();
		model.put("user", user);
	}
	
	/**
	 * 添加用户
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "create", method = RequestMethod.POST)
	public String create(@ModelAttribute("user") User user) {
		UUID uuid = UUID.randomUUID();
		String id = uuid.toString().replaceAll("\\-", "");
		user.setId(id);
		Date now = new Date();
		user.setCreateTime(now.toLocaleString());
		String userKey = String.format(REDIS_KEY_USER, id);
		
		// 一条数据是一个hash，按每个字段存储
		zhashOps.put(userKey, User.PRO_ID, id);
		zhashOps.put(userKey, User.PRO_NAME, user.getName());
		zhashOps.put(userKey, User.PRO_CREATETIME, user.getCreateTime());
		
		// 登记主键，以时间戳作为score，问题：row怎么来的？
		zsetOps.add(REDIS_KEY_USER_IDS, id + "", now.getTime());
		return "redirect:/user.htm";
	}
	
	/**
	 * 删除用户
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "delete", method = RequestMethod.GET)
	public String delete(@RequestParam(value = "id", required = false) String id) {
		String userKey = String.format(REDIS_KEY_USER, id + "");
		redisTemplate.delete(userKey);
		zsetOps.remove(REDIS_KEY_USER_IDS, id);
		return "redirect:/user.htm";
	}
}
