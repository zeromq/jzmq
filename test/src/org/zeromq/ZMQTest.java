package org.zeromq;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author Cliff Evans
 */
public class ZMQTest
{

  /**
   * Test method for {@link org.zeromq.ZMQ#makeVersion(int, int, int)}.
   */
  @Test
  public void testMakeVersion ()
  {
    assertEquals ( ZMQ.getFullVersion (),
                   ZMQ.makeVersion ( ZMQ.getMajorVersion (),
                                     ZMQ.getMinorVersion (),
                                     ZMQ.getPatchVersion () ) );
  }


  /**
   * Test method for {@link org.zeromq.ZMQ#getVersionString()}.
   */
  @Test
  public void testGetVersion ()
  {
    assertEquals ( ZMQ.getMajorVersion () + "." + ZMQ.getMinorVersion () + "." + ZMQ.getPatchVersion (),
                   ZMQ.getVersionString () );
  }

}
