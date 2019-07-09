package com.hbueluojing.merge.config;

import com.buraequete.orikautomation.OrikautomationMain;
import com.google.common.collect.Lists;
import java.util.List;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.DefaultFieldMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Configuration
@Import(OrikautomationMain.class)
public class BeanMapperConfiguration {
	@Component
	public static class PatchMapper extends ListAwareMapper {

		public PatchMapper() {
			super();
		}

		@Override
		protected void configureFactoryBuilder(DefaultMapperFactory.Builder factoryBuilder) {
			factoryBuilder.mapNulls(false).build();
		}
	}

	public static class ListAwareMapper extends ConfigurableMapper {

		public ListAwareMapper() {
			super();
		}

		@Override
		protected void configure(MapperFactory factory) {
			ListMapper listMapper = new ListMapper();
			factory.classMap(listMapper.getAType(), listMapper.getBType())
					.byDefault(new DefaultFieldMapper[0])
					.customize(listMapper).register();
		}
	}

	public static class ListMapper extends CustomMapper<List, List> {

		@Override
		public void mapAtoB(List objects, List objects2, MappingContext context) {
			map(objects, objects2);
		}

		@Override
		public void mapBtoA(List objects, List objects2, MappingContext context) {
			map(objects, objects2);
		}

		private void map(List objects, List objects2) {
			List temp = Lists.newArrayList(objects);
			objects2.clear();
			objects2.addAll(temp);
		}
	}
}
