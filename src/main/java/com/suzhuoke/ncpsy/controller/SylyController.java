package com.suzhuoke.ncpsy.controller;


import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.suzhuoke.ncpsy.dao.SylyMapper;
import com.suzhuoke.ncpsy.model.Ncp;
import com.suzhuoke.ncpsy.model.Syly;
import com.suzhuoke.ncpsy.model.SylyCountNcpGroupByNcpid;
import com.suzhuoke.ncpsy.service.ISylyService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author RobinYoung10
 * @since 2019-02-22
 */
@Controller
@RequestMapping("/handle")
public class SylyController {
	
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private ISylyService sylyService;
	
	@Autowired
	private SylyMapper sylyMapper;
	
	/**
	 * 溯源来源计数
	 * @param syly
	 * @param request
	 * @return
	 */
	@RequestMapping("/source/count")
	@ResponseBody
	public boolean sourceCount(@RequestBody Syly syly, HttpServletRequest request) {
		logger.info("/handle/source/count===> syly={}", syly);
		
		//查询syly总数,即溯源总数
		QueryWrapper<Syly> countQueryWrapper = new QueryWrapper<>();
		countQueryWrapper.eq("syqyid", syly.getSyqyid());
		int count = sylyService.count(countQueryWrapper) + 1;
		//获取当前日期，设置溯源时间
		Date now = new Date();
		syly.setSysj(now);
		//设置溯源id
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String newNo = df.format(now);
		String syid = "syly-" + newNo + "-" + count;
		syly.setSyid(syid);
		//设置溯源ip
		String syip = request.getRemoteAddr();
		syly.setSyip(syip);
		
		boolean flag = sylyService.save(syly);
		return flag;
	}
	
	/**
	 * 溯源列表
	 * @param syly
	 * @param page
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/source/list")
	@ResponseBody
	public Map sourceList(Syly syly, @RequestParam int page, @RequestParam int limit) throws Exception {
		logger.info("/handle/source/list===> syly={}", syly);
		logger.info("page = {}", page);
		logger.info("limit = {}", limit);
		QueryWrapper<Syly> sylyQueryWrapper = new QueryWrapper<>();
		//遍历syly对象的属性
		Field field[] = syly.getClass().getDeclaredFields();
		for(int i = 0; i < field.length; i++) {
			//获取属性名
			String name = field[i].getName();
			//将属性的首字符大写，方便构造get，set方法
			String getterName = name.substring(0,1).toUpperCase()+name.substring(1);
			//获取属性的类型
			String type = field[i].getGenericType().toString();
			//根据类型做操作
			if (type.equals("class java.lang.String")) {
				//获得getter方法
				Method m = syly.getClass().getMethod("get" + getterName);
				//调用getter方法
				String value = (String) m.invoke(syly);
				//如果非空，则加入查询条件
				if (value != null) {
					sylyQueryWrapper.eq(name, value);
				}
			}
		}
		
		List<Syly> sylyList = sylyService.list(sylyQueryWrapper);
		logger.info("=========={}", sylyList);
		//查询到的总量，返回数据要用
		int count = sylyList.size();
		//list截取分页的索引
		int fromIndex = (page-1)*limit;
		int toIndex = page * limit;
		//截取分页数据
		if(page*limit > count) {
			toIndex = count;
		}
		sylyList = sylyList.subList(fromIndex, toIndex);
		
		Map response = new HashMap();
		response.put("code", 0);
		response.put("msg", "");
		response.put("count", count);
		response.put("data", sylyList);
		
		return response;
	}
	
	/**
	 * 获取7天溯源数据
	 * @param syly
	 * @return
	 */
	@RequestMapping("/source/line")
	@ResponseBody
	public Map sourceChart(@RequestBody Syly syly) {
		logger.info("/handle/source/line===> syly={}", syly);
		//groud by 溯源时间查询list
		QueryWrapper<Syly> sylyQueryWrapper = new QueryWrapper<>();
		sylyQueryWrapper.groupBy("sysj").eq("syqyid", syly.getSyqyid());
		List<Syly> sylyList = sylyService.list(sylyQueryWrapper);
		//map对象用来储存返回数据
		Map map = new HashMap();
		//新建sysjList和counts，用于保存时间和访问数
		List<String> sysjList = new ArrayList<>();
		int[] counts = new int[sylyList.size()];
		if(sylyList.size() >= 7) {
			//截取list最后七项
			sylyList = sylyList.subList(sylyList.size()-7, sylyList.size());
			int num = 0;
			for(Syly item : sylyList) {
				//依次查询溯源次数
				QueryWrapper<Syly> countQueryWrapper = new QueryWrapper<>();
				countQueryWrapper.eq("sysj", item.getSysj()).eq("syqyid", syly.getSyqyid());
				int count = sylyService.count(countQueryWrapper);
				counts[num] = count;
				num++;
				//溯源时间转字符串
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String sysjString = df.format(item.getSysj());
				sysjList.add(sysjString);
			}
			map.put("sysjList", sysjList);
			map.put("counts", counts);
			return map;
		} else {
			sylyList = sylyList.subList(0, sylyList.size());
			int num = 0;
			for(Syly item : sylyList) {
				//依次查询溯源次数
				QueryWrapper<Syly> countQueryWrapper = new QueryWrapper<>();
				countQueryWrapper.eq("sysj", item.getSysj()).eq("syqyid", syly.getSyqyid());
				int count = sylyService.count(countQueryWrapper);
				counts[num] = count;
				num++;
				//溯源时间转字符串
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String sysjString = df.format(item.getSysj());
				sysjList.add(sysjString);
			}
			map.put("sysjList", sysjList);
			map.put("counts", counts);
			return map;
		}
	}
	
	@RequestMapping("/source/pie")
	@ResponseBody
	public Map sourcePie(@RequestBody Syly syly) {
		logger.info("/handle/source/pie===> syly={}", syly);
		//获取溯源总数
		QueryWrapper<Syly> sylyQueryWrapper = new QueryWrapper<>();
		sylyQueryWrapper.eq("syqyid", syly.getSyqyid());
		int count = sylyService.count(sylyQueryWrapper);
		int alreadyGet = 0;
		//获取最大溯源量的4个ncp
		List<SylyCountNcpGroupByNcpid> dataList = sylyMapper.selectSylyCountNcpGroupByNcpid(syly.getSyqyid());
		//新建两个列表对象储存返回数据
		List<String> ncpmcList = new ArrayList<>();
		List<Integer> countList = new ArrayList<>();
		//插入查询到的数据到list
		for(SylyCountNcpGroupByNcpid item : dataList) {
			ncpmcList.add(item.getNcpmc());
			countList.add(item.getCount());
			alreadyGet += item.getCount();
		}
		ncpmcList.add("其他");
		countList.add(count-alreadyGet);
		Map map = new HashMap();
		map.put("ncpmcList", ncpmcList);
		map.put("countList", countList);
		return map;
	}
	
	@RequestMapping("/source/total")
	@ResponseBody
	public List<String> sourceTotal(@RequestBody Syly syly) {
		logger.info("/handle/source/total===> syly={}", syly);
		//获取溯源总数
		QueryWrapper<Syly> sylyQueryWrapper = new QueryWrapper<>();
		sylyQueryWrapper.eq("syqyid", syly.getSyqyid());
		int count = sylyService.count(sylyQueryWrapper);
		List<String> list = new ArrayList<>();
		list.add(count+"");
		return list;
	}
	
	@RequestMapping("/source/today")
	@ResponseBody
	public List<String> sourceToday(@RequestBody Syly syly) {
		logger.info("/handle/source/today===> syly={}", syly);
		//获取今天日期
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String todayString = df.format(new Date());
		//获取昨天日期
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		String yesterdayString = df.format(new Date(new Date().getTime()-86400000L));
		//查询今天总数
		QueryWrapper<Syly> todayQueryWrapper = new QueryWrapper<>();
		todayQueryWrapper.eq("syqyid", syly.getSyqyid()).eq("sysj", todayString);
		int todayCount = sylyService.count(todayQueryWrapper);
		//查询昨天总数
		QueryWrapper<Syly> yesterdayQueryWrapper = new QueryWrapper<>();
		yesterdayQueryWrapper.eq("syqyid", syly.getSyqyid()).eq("sysj", yesterdayString);
		int yesterdayCount = sylyService.count(yesterdayQueryWrapper);
		//算出同比增长比例
		float rise = ((float)todayCount - (float)yesterdayCount) / (float)yesterdayCount * 100;
		
		List<String> list = new ArrayList<>();
		list.add(todayCount+"");
		list.add(rise+"%");
		return list;
	}

}

