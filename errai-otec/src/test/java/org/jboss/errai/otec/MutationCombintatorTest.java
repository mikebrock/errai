package org.jboss.errai.otec;

import junit.framework.Assert;
import org.jboss.errai.otec.client.Transformer;
import org.jboss.errai.otec.client.mutation.Mutation;
import org.jboss.errai.otec.client.mutation.MutationType;
import org.jboss.errai.otec.client.mutation.StringMutation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Brock
 */
public class MutationCombintatorTest {

  @Test
  public void testConsecutiveInsertsCombination() {
    final List<Mutation> mutationList = new ArrayList<Mutation>();

    mutationList.add(StringMutation.of(MutationType.Insert, 0, "foo"));
    mutationList.add(StringMutation.of(MutationType.Insert, 3, "bar"));
    mutationList.add(StringMutation.of(MutationType.Insert, 6, "!"));

    final Mutation mutation = Transformer.mutationCombinitator(mutationList);
    Assert.assertNotNull(mutation);
    Assert.assertTrue(mutation.getType() == MutationType.Insert);
    Assert.assertEquals("foobar!", mutation.getData());
  }

  @Test
  public void testConsecutiveInsertsWithTrailingDeleteCombination() {
    final List<Mutation> mutationList = new ArrayList<Mutation>();

    mutationList.add(StringMutation.of(MutationType.Insert, 10, "foo"));
    mutationList.add(StringMutation.of(MutationType.Insert, 13, "bar"));
    mutationList.add(StringMutation.of(MutationType.Insert, 16, "!"));
    mutationList.add(StringMutation.of(MutationType.Delete, 14, "ar"));

    final Mutation mutation = Transformer.mutationCombinitator(mutationList);
    Assert.assertNotNull(mutation);
    Assert.assertTrue(mutation.getType() == MutationType.Insert);
    Assert.assertEquals("foob!", mutation.getData());
  }


  @Test
  public void testConsecutiveInsertsWithTrailingDeleteCombination2() {
    final List<Mutation> mutationList = new ArrayList<Mutation>();

    mutationList.add(StringMutation.of(MutationType.Insert, 10, "foo"));
    mutationList.add(StringMutation.of(MutationType.Insert, 13, "bar"));
    mutationList.add(StringMutation.of(MutationType.Insert, 16, "!"));
    mutationList.add(StringMutation.of(MutationType.Delete, 13, "bar!"));

    final Mutation mutation = Transformer.mutationCombinitator(mutationList);
    Assert.assertNotNull(mutation);
    Assert.assertTrue(mutation.getType() == MutationType.Insert);
    Assert.assertEquals("foo", mutation.getData());
  }

  @Test
  public void testInsertInsertAppendCombination() {
    final List<Mutation> mutationList = new ArrayList<Mutation>();

    mutationList.add(StringMutation.of(MutationType.Insert, 0, "X"));
    mutationList.add(StringMutation.of(MutationType.Insert, 0, "AB"));
    mutationList.add(StringMutation.of(MutationType.Insert, 3, "YZ"));

    final Mutation mutation = Transformer.mutationCombinitator(mutationList);

    Assert.assertNotNull(mutation);
    Assert.assertTrue(mutation.getType() == MutationType.Insert);
    Assert.assertEquals("ABXYZ", mutation.getData());
  }
}
